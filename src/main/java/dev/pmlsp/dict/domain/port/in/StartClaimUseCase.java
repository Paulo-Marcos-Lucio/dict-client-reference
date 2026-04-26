package dev.pmlsp.dict.domain.port.in;

import dev.pmlsp.dict.domain.model.Account;
import dev.pmlsp.dict.domain.model.Claim;
import dev.pmlsp.dict.domain.model.ClaimType;
import dev.pmlsp.dict.domain.model.Ispb;
import dev.pmlsp.dict.domain.model.Owner;
import dev.pmlsp.dict.domain.model.PixKey;

public interface StartClaimUseCase {

    Claim start(StartClaimCommand cmd);

    record StartClaimCommand(
            ClaimType type,
            PixKey key,
            Account claimerAccount,
            Owner claimerOwner,
            Ispb claimerIspb) {}
}
