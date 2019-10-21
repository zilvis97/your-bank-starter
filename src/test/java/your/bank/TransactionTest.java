package your.bank;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class TransactionTest {
    private Transaction transaction;
    private Account sender;
    private Account recipient;

    @BeforeEach
    void init() {
        sender = new Account("Sendy McSendface", new BigDecimal("50.00"), "GBP");
        recipient = new Account("Getty McGetface", new BigDecimal("50.00"), "GBP");

        transaction = new Transaction("ffffffff-ffff-ffff-ffff-ffffffffffff", sender, recipient, new BigDecimal("20"));
    }

    @Test
    void transactionInitsToPending() {
        assertEquals(Transaction.TransactionStatus.PENDING, transaction.getStatus());
    }

    @Test
    void insufficientFunds() {
        sender.withdraw(new BigDecimal("50.00"));
        transaction.approve();

        assertEquals(Transaction.TransactionStatus.FAILED, transaction.getStatus());

        assertEquals(1, sender.getTransactionsFailed());
        assertEquals(1, recipient.getTransactionsFailed());

        assertEquals(new BigDecimal("0.00"), sender.getCurrentBalance());
        assertEquals(new BigDecimal("50.00"), recipient.getCurrentBalance());

    }

    @Test
    void approvesHappily() {
        transaction.approve();

        assertEquals(Transaction.TransactionStatus.AUTHORIZED, transaction.getStatus());

        assertEquals(1, sender.getTransactionsProcessed());
        assertEquals(1, recipient.getTransactionsProcessed());

        assertEquals(new BigDecimal("30.00"), sender.getCurrentBalance());
        assertEquals(new BigDecimal("70.00"), recipient.getCurrentBalance());

    }

    @Test
    void cantReapproveAuthorizedTransaction() {
        transaction.approve();
        transaction.approve();

        assertEquals(1, sender.getTransactionsFailed());
        assertEquals(1, recipient.getTransactionsFailed());

        assertEquals(1, sender.getTransactionsProcessed());
        assertEquals(1, recipient.getTransactionsProcessed());

        assertEquals(new BigDecimal("30.00"), sender.getCurrentBalance());
        assertEquals(new BigDecimal("70.00"), recipient.getCurrentBalance());
    }

    @Test
    void markFraudulent() {
        transaction.revoke(true);
        assertEquals(Transaction.TransactionStatus.FRAUDULENT, transaction.getStatus());

        assertTrue(sender.getFraudulentActivity());
        assertTrue(recipient.getFraudulentActivity());

        assertEquals(1, sender.getTransactionsFailed());
        assertEquals(1, recipient.getTransactionsFailed());
    }

    @Test
    void fraudulentTransactionDoesntClear() {
        transaction.revoke(true);
        transaction.approve();

        assertEquals(2, sender.getTransactionsFailed());
        assertEquals(2, recipient.getTransactionsFailed());

        assertEquals(0, sender.getTransactionsProcessed());
        assertEquals(0, recipient.getTransactionsProcessed());

        assertEquals(new BigDecimal("50.00"), sender.getCurrentBalance());
        assertEquals(new BigDecimal("50.00"), recipient.getCurrentBalance());
    }

}