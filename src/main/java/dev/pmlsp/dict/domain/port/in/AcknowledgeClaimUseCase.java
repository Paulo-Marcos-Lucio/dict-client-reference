package dev.pmlsp.dict.domain.port.in;

import dev.pmlsp.dict.domain.model.Claim;
import dev.pmlsp.dict.domain.model.Ispb;

import java.util.UUID;

public interface AcknowledgeClaimUseCase {

    Claim acknowledge(UUID claimId, Ispb donorIspb);
}
