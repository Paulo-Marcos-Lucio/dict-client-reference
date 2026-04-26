package dev.pmlsp.dict.domain.port.in;

import dev.pmlsp.dict.domain.model.Claim;
import dev.pmlsp.dict.domain.model.Ispb;
import dev.pmlsp.dict.domain.model.Reason;

import java.util.UUID;

public interface CancelClaimUseCase {

    Claim cancel(UUID claimId, Ispb requesterIspb, Reason reason);
}
