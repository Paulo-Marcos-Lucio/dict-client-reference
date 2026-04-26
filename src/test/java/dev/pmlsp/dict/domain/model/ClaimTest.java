package dev.pmlsp.dict.domain.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ClaimTest {

    private static final PixKey KEY = new PixKey(PixKeyType.PHONE, "+5511987654321");
    private static final Account CLAIMER_ACC = new Account(
            Ispb.of("87654321"), "0001", "0000999888", AccountType.CACC);
    private static final Owner CLAIMER_OWNER = Owner.naturalPerson("12345678901", "Alice");
    private static final Ispb CLAIMER = Ispb.of("87654321");
    private static final Ispb DONOR = Ispb.of("12345678");

    @Test
    void openInitializesAtOpenStatusWithComputedDeadline() {
        Claim claim = Claim.open(ClaimType.PORTABILITY, KEY, CLAIMER_ACC, CLAIMER_OWNER, CLAIMER, DONOR);

        assertThat(claim.status()).isEqualTo(ClaimStatus.OPEN);
        assertThat(claim.resolutionDeadline())
                .isAfter(claim.requestedAt())
                .isBefore(claim.requestedAt().plusSeconds(8 * 24 * 3600));
    }

    @Test
    void happyPathFlowsThroughEachState() {
        Claim claim = Claim.open(ClaimType.OWNERSHIP, KEY, CLAIMER_ACC, CLAIMER_OWNER, CLAIMER, DONOR)
                .acknowledge()
                .confirm()
                .complete();

        assertThat(claim.status()).isEqualTo(ClaimStatus.COMPLETED);
        assertThat(claim.completedAtOpt()).isPresent();
    }

    @Test
    void cannotCompleteFromOpenDirectly() {
        Claim claim = Claim.open(ClaimType.PORTABILITY, KEY, CLAIMER_ACC, CLAIMER_OWNER, CLAIMER, DONOR);

        assertThatThrownBy(claim::complete)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("complete");
    }

    @Test
    void cancelTransitionsToCancelledFromAnyPending() {
        Claim cancelledFromOpen = Claim.open(ClaimType.PORTABILITY, KEY, CLAIMER_ACC, CLAIMER_OWNER, CLAIMER, DONOR)
                .cancel(Reason.USER_REQUESTED);
        Claim cancelledFromConfirmed = Claim.open(ClaimType.OWNERSHIP, KEY, CLAIMER_ACC, CLAIMER_OWNER, CLAIMER, DONOR)
                .acknowledge().confirm().cancel(Reason.RECONCILIATION);

        assertThat(cancelledFromOpen.status()).isEqualTo(ClaimStatus.CANCELLED);
        assertThat(cancelledFromOpen.cancellationReasonOpt()).contains(Reason.USER_REQUESTED);
        assertThat(cancelledFromConfirmed.status()).isEqualTo(ClaimStatus.CANCELLED);
    }

    @Test
    void cannotCancelTerminalClaim() {
        Claim completed = Claim.open(ClaimType.OWNERSHIP, KEY, CLAIMER_ACC, CLAIMER_OWNER, CLAIMER, DONOR)
                .acknowledge().confirm().complete();

        assertThatThrownBy(() -> completed.cancel(Reason.USER_REQUESTED))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("terminal");
    }

    @Test
    void rejectsClaimerEqualToDonor() {
        assertThatThrownBy(() ->
                Claim.open(ClaimType.PORTABILITY, KEY, CLAIMER_ACC, CLAIMER_OWNER, CLAIMER, CLAIMER))
                .hasMessageContaining("must differ");
    }
}
