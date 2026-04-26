package dev.pmlsp.dict.domain.port.in;

import dev.pmlsp.dict.domain.model.DictEntry;
import dev.pmlsp.dict.domain.model.Ispb;
import dev.pmlsp.dict.domain.model.PixKey;

public interface LookupKeyUseCase {

    DictEntry lookup(LookupQuery query);

    record LookupQuery(PixKey key, Ispb payerIspb) {}
}
