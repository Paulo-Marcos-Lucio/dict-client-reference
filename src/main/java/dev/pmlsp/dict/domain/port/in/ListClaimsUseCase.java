package dev.pmlsp.dict.domain.port.in;

import dev.pmlsp.dict.domain.model.Claim;
import dev.pmlsp.dict.domain.model.ClaimRole;
import dev.pmlsp.dict.domain.model.Ispb;

import java.util.List;

public interface ListClaimsUseCase {

    List<Claim> list(ListClaimsQuery query);

    record ListClaimsQuery(ClaimRole role, Ispb ispb) {}
}
