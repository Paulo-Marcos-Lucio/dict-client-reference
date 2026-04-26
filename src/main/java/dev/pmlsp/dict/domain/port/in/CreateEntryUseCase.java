package dev.pmlsp.dict.domain.port.in;

import dev.pmlsp.dict.domain.model.Account;
import dev.pmlsp.dict.domain.model.DictEntry;
import dev.pmlsp.dict.domain.model.Ispb;
import dev.pmlsp.dict.domain.model.Owner;
import dev.pmlsp.dict.domain.model.PixKey;

public interface CreateEntryUseCase {

    DictEntry create(CreateEntryCommand cmd);

    record CreateEntryCommand(PixKey key, Account account, Owner owner, Ispb requesterIspb) {}
}
