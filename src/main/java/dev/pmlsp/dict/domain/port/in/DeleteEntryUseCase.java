package dev.pmlsp.dict.domain.port.in;

import dev.pmlsp.dict.domain.model.Ispb;
import dev.pmlsp.dict.domain.model.PixKey;
import dev.pmlsp.dict.domain.model.Reason;

public interface DeleteEntryUseCase {

    void delete(DeleteEntryCommand cmd);

    record DeleteEntryCommand(PixKey key, Reason reason, Ispb requesterIspb) {}
}
