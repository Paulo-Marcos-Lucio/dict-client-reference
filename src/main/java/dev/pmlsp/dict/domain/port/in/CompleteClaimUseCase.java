package dev.pmlsp.dict.domain.port.in;

import dev.pmlsp.dict.domain.model.Claim;
import dev.pmlsp.dict.domain.model.Ispb;

import java.util.UUID;

public interface CompleteClaimUseCase {

    Claim complete(UUID claimId, Ispb requesterIspb);
}
