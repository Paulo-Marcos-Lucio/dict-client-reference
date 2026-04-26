# Flow — Portability claim (chave muda de banco)

```mermaid
sequenceDiagram
    autonumber
    participant U as Titular (cliente final)
    participant Claimer as Banco novo (claimer)
    participant DICT as BCB / DICT
    participant Donor as Banco antigo (donor)
    participant CC as DictClient (este projeto)

    U->>Claimer: "Quero migrar minha chave Pix"
    Claimer->>CC: StartClaimUseCase.start({type: PORTABILITY, key, claimerAccount, claimerOwner, claimerIspb})
    CC->>DICT: POST /claims (body com tipo, chave, dados claimer)
    DICT-->>CC: ClaimResponse{status: OPEN, deadline: now+7d}
    CC->>CC: cache.invalidate(key) (chave em disputa)
    CC-->>Claimer: Claim{status: OPEN}

    Note over DICT,Donor: DICT notifica banco antigo

    Donor->>CC: AcknowledgeClaimUseCase.acknowledge(claimId, donorIspb)
    CC->>DICT: POST /claims/{id}/acknowledge
    DICT-->>CC: Claim{status: WAITING_RESOLUTION}

    alt Titular confirma com banco antigo
        Donor->>CC: complete via canal proprio (não modelado)
        Note right of Donor: Em produção, o titular confirma<br/>no app do banco antigo
        Donor->>CC: gateway interno -> DICT confirma
        CC->>DICT: (transição WAITING_RESOLUTION → CONFIRMED)
        DICT-->>CC: Claim{status: CONFIRMED}

        Claimer->>CC: CompleteClaimUseCase.complete(claimId, claimerIspb)
        CC->>DICT: POST /claims/{id}/complete
        DICT-->>CC: Claim{status: COMPLETED, completedAt: now}
        CC->>CC: cache.invalidate(key)
        CC-->>Claimer: Claim{status: COMPLETED}
    else Donor recusa ou timeout (deadline expirado)
        Donor->>CC: CancelClaimUseCase.cancel(claimId, donorIspb, REJECTED_BY_PSP)
        CC->>DICT: POST /claims/{id}/cancel
        DICT-->>CC: Claim{status: CANCELLED, reason: REJECTED_BY_PSP}
        CC-->>Claimer: Claim{status: CANCELLED}
    end
```

## Pontos críticos

- **Janela de 7 dias** (definida em `ClaimType.PORTABILITY.resolutionWindow()`).
- **Cache da chave é invalidado** ao iniciar e ao completar o claim — qualquer cliente que tinha a chave em cache buscará no DICT (que retornará a entrada com `openClaimType` durante a janela).
- **Claim em aberto bloqueia novo claim** sobre a mesma chave — DICT retorna 409 + `CLAIM_CONFLICT`, mapeado para `ClaimConflictException`.
- **Resolution deadline é informativa** — o DICT cancela automaticamente o claim se não houver resolução até `requestedAt + 7d`. O cliente não precisa rodar timer próprio, mas pode listar claims abertas perto do deadline para tomar ação.

## Diferenças vs Ownership claim

`OWNERSHIP` segue exatamente o mesmo fluxo, mas:
- Janela é de 30 dias.
- A confirmação envolve revisão antifraude do donor — não é apenas "autorizar" do titular.
- O risco operacional é maior (alguém alegando posse contra o titular legítimo) — o donor deve rejeitar liberalmente quando há suspeita.
