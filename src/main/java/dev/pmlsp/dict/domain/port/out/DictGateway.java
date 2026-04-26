package dev.pmlsp.dict.domain.port.out;

import dev.pmlsp.dict.domain.model.Account;
import dev.pmlsp.dict.domain.model.Claim;
import dev.pmlsp.dict.domain.model.ClaimRole;
import dev.pmlsp.dict.domain.model.ClaimType;
import dev.pmlsp.dict.domain.model.DictEntry;
import dev.pmlsp.dict.domain.model.Ispb;
import dev.pmlsp.dict.domain.model.Owner;
import dev.pmlsp.dict.domain.model.PixKey;
import dev.pmlsp.dict.domain.model.Reason;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Output port abstracting the BCB DICT API. Implementations carry the HTTP, mTLS,
 * serialization, retry and circuit-breaker concerns; callers (use cases) stay pure.
 */
public interface DictGateway {

    Optional<DictEntry> lookup(PixKey key, Ispb requesterIspb);

    DictEntry createEntry(DictEntry entry, Ispb requesterIspb);

    void deleteEntry(PixKey key, Reason reason, Ispb requesterIspb);

    Claim startClaim(
            ClaimType type,
            PixKey key,
            Account claimerAccount,
            Owner claimerOwner,
            Ispb claimerIspb);

    Claim acknowledgeClaim(UUID claimId, Ispb donorIspb);

    Claim completeClaim(UUID claimId, Ispb requesterIspb);

    Claim cancelClaim(UUID claimId, Ispb requesterIspb, Reason reason);

    List<Claim> listClaims(ClaimRole role, Ispb ispb);
}
