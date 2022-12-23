import java.util.concurrent.locks.ReentrantLock


/**
 * Bank implementation.
 *
 * :TODO: This implementation has to be made thread-safe.
 *
 * @author :TODO: LastName FirstName
 */
class BankImpl(n: Int) : Bank {
    private val accounts: Array<Account> = Array(n) { Account() }

    override val numberOfAccounts: Int
        get() = accounts.size

    /**
     * :TODO: This method has to be made thread-safe.
     */
    override fun getAmount(index: Int): Long {
        accounts[index].lock()
        val amount = accounts[index].amount
        accounts[index].unlock()

        return amount
    }

    /**
     * :TODO: This method has to be made thread-safe.
     */
    override val totalAmount: Long
        get() {
            for (account in accounts) {
                account.lock()
            }

            val sum: Long = accounts.sumOf { account -> account.amount }

            for (account in accounts) {
                account.unlock()
            }

            return sum
        }

    /**
     * :TODO: This method has to be made thread-safe.
     */
    override fun deposit(index: Int, amount: Long): Long {
        require(amount > 0) { "Invalid amount: $amount" }

        val answer: Long
        val account = accounts[index]
        account.lock()
        if (amount > Bank.MAX_AMOUNT || account.amount + amount > Bank.MAX_AMOUNT) {
            account.unlock()
            throw IllegalStateException("Overflow")
        }

        account.amount += amount
        answer = account.amount
        account.unlock()
        return answer
    }

    /**
     * :TODO: This method has to be made thread-safe.
     */
    override fun withdraw(index: Int, amount: Long): Long {
        require(amount > 0) { "Invalid amount: $amount" }

        val answer: Long
        val account = accounts[index]
        account.lock()
        if (account.amount - amount < 0) {
            account.unlock()
            throw IllegalStateException("Underflow")
        }

        account.amount -= amount
        answer = account.amount
        account.unlock()
        return answer
    }

    /**
     * :TODO: This method has to be made thread-safe.
     */
    override fun transfer(fromIndex: Int, toIndex: Int, amount: Long) {
        require(amount > 0) { "Invalid amount: $amount" }
        require(fromIndex != toIndex) { "fromIndex == toIndex" }

        val from = accounts[fromIndex]
        val to = accounts[toIndex]
        if (fromIndex < toIndex) {
            from.lock()
            to.lock()
        } else {
            to.lock()
            from.lock()
        }

        try {
            check(amount <= from.amount) { "Underflow" }
            check(!(amount > Bank.MAX_AMOUNT || to.amount + amount > Bank.MAX_AMOUNT)) { "Overflow" }
            from.amount -= amount
            to.amount += amount
        } finally {
            if (fromIndex < toIndex) {
                to.unlock()
                from.unlock()
            } else {
                from.unlock()
                to.unlock()
            }
        }
    }

    /**
     * Private account data structure.
     */
    class Account {
        /**
         * Amount of funds in this account.
         */
        var amount: Long = 0
        var lock = ReentrantLock()

        fun lock() {
            lock.lock()
        }

        fun unlock() {
            lock.unlock()
        }
    }
}
