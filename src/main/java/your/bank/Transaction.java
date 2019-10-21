package your.bank;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.UUID;

public class Transaction {
    private final BigDecimal transactionAmount;
    private final UUID transactionID;

    private final Account sender;
    private final Account recipient;
    private TransactionStatus status;

    private final Logger logger = LoggerFactory.getLogger(App.class);

    /**
     * @param sender            The Account of the party sending money
     * @param recipient         The Account of the party receiving money
     * @param transactionAmount BigDecimal of the amount being exchanged
     */
    public Transaction(String id, Account sender, Account recipient, BigDecimal transactionAmount) {
        this.transactionAmount = transactionAmount;
        this.transactionID = UUID.fromString(id);
        this.sender = sender;
        this.recipient = recipient;
        this.status = TransactionStatus.PENDING;
    }

    public BigDecimal getTransactionAmount() {
        return transactionAmount;
    }

    public Account getSender() {
        return sender;
    }

    public Account getRecipient() {
        return recipient;
    }

    public UUID getTransactionID() {
        return transactionID;
    }

    public TransactionStatus getStatus() {
        return status;
    }

    /**
     * If the transaction status is PENDING, updates transaction status to AUTHORIZED and actually carries out the transaction.
     * If the transaction status is FAILED, tries to re-approve.
     * If the transaction status is already AUTHORIZED, FAILED, or FRAUDULENT/REVOKED, does nothing.
     */
    public void approve() {
        boolean withdrawSuccess;
        boolean depositSuccess;

        switch (status) {
            case PENDING:
                withdrawSuccess = sender.withdraw(transactionAmount);

                if (!withdrawSuccess) {
                    status = TransactionStatus.FAILED;
                    sender.incrementTransactionsFailed();
                    recipient.incrementTransactionsFailed();
                    return;
                }

                depositSuccess = recipient.deposit(transactionAmount);

                if (!depositSuccess) {
                    status = TransactionStatus.FAILED;
                    sender.deposit(transactionAmount);
                    sender.incrementTransactionsFailed();
                    recipient.incrementTransactionsFailed();
                    return;
                }

                sender.incrementTransactionsProcessed();
                recipient.incrementTransactionsProcessed();
                status = TransactionStatus.AUTHORIZED;
                break;

            case FRAUDULENT:
                logger.warn("Caught attempt to re-approve revoked transaction with ID " + transactionID.toString() + " which has been previously marked as fraudulent.");
                //todo - this seems like the kind of thing you'd want to escalate somehow
                sender.incrementTransactionsFailed();
                recipient.incrementTransactionsFailed();
                break;

            case REVOKED:
                logger.warn("Caught attempt to approve revoked transaction with ID " + transactionID.toString());
                sender.incrementTransactionsFailed();
                recipient.incrementTransactionsFailed();
                break;

            case AUTHORIZED:
                logger.warn("Caught attempt to approve already authorized transaction with ID " + transactionID.toString());
                sender.incrementTransactionsFailed();
                recipient.incrementTransactionsFailed();
                break;

            case FAILED:
                logger.warn("Caught attempt to approve transaction with ID " + transactionID.toString() + ". Please make a new transaction.");
                sender.incrementTransactionsFailed();
                recipient.incrementTransactionsFailed();
        }
    }

    /**
     * Undoes the transaction, then sets the transaction status to REVOKED or FRAUDULENT as appropriate.
     *
     * @param fraudulent Whether the transaction was fraudulent or not
     */
    public void revoke(boolean fraudulent) {
        switch (status) {
            case AUTHORIZED:
                sender.deposit(transactionAmount);
                recipient.withdraw(transactionAmount);
                sender.decrementTransactionsProcessed();
                recipient.decrementTransactionsProcessed();
                logger.info("Revoked and reverted previously approved transaction " + transactionID);
                break;

            case PENDING:
                logger.info("Transaction " + transactionID.toString() + " hasn't been approved yet, it was caught in time.");
                break;

            case FRAUDULENT:
                logger.info("Transaction " + transactionID.toString() + " has already been revoked and marked as fraudulent.");
                break;

            case REVOKED:
                logger.info("Transaction " + transactionID.toString() + " has already been revoked.");
                break;

            case FAILED:
                logger.info("Transaction " + transactionID.toString() + " failed in the first place.");
        }

        if (fraudulent) {
            status = TransactionStatus.FRAUDULENT;
            sender.setFraudulentActivity(true);
            recipient.setFraudulentActivity(true);
        } else {
            status = TransactionStatus.REVOKED;
        }

        sender.incrementTransactionsFailed();
        recipient.incrementTransactionsFailed();
    }


    @Override
    public boolean equals(Object o){
        if(o instanceof Transaction){
            Transaction t = (Transaction) o;
            if(t.transactionID.equals(this.transactionID)){
                if(t.sender.equals(this.sender) &&
                        t.recipient.equals(this.recipient) &&
                        t.transactionAmount.equals(this.transactionAmount) &&
                        t.status.equals(this.status)) {
                    return true;
                } else {
                    logger.error("Comparing 2 transactions with the same ID but different data. If you're comparing the status of the same transaction at different points in time, don't use ::equals(). Otherwise, this may be a UUID collision.");
                }
            }
        }

        return false;
    }


    public enum TransactionStatus {
        PENDING,
        AUTHORIZED,
        FAILED,
        REVOKED,
        FRAUDULENT;
    }

}
