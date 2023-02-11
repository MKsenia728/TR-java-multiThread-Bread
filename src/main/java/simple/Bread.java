package simple;

import lombok.Getter;
import lombok.Setter;

public class Bread {
    public static void main(String[] args) {
        Market market = new Market();
        Producer producer = new Producer(market);
        Seller seller = new Seller(market);
        Buyer buyer = new Buyer(market);

        Thread threadProducer = new Thread(producer);
        Thread threadSeller = new Thread(seller);
        Thread threadBuyer = new Thread(buyer);

        threadBuyer.setName("ThreadBuyer");
        threadSeller.setName("ThreadSeller");
        threadProducer.setName("ThreadProducer");

        threadProducer.start();
        threadSeller.start();
        threadBuyer.start();
    }
}

@Getter
@Setter
class Market {

    private final int countStage = 13;
    private int storageProducer = 0;
    private int storageSeller = 0;
    private boolean isProducing = true;
    private boolean isSelling = false;
    private boolean isBuying = false;

    public synchronized void produceBread() {
        while (!isProducing) {
            try {
                wait();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        storageProducer++;
        System.out.println("Producer has produced a bread  --> ");
        System.out.println("Bread quantity is: " + storageProducer);
        if (storageProducer == 5) {
            isProducing = false;
            isSelling = true;
            notifyAll();
        }
    }

    public synchronized void sellBread() {
        while (!isSelling) {
            try {
                wait();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        if (storageSeller == 0 && storageProducer == 0) {
            isProducing = true;
            isSelling = false;
            notifyAll();
        } else {
            storageProducer--;
            storageSeller++;
            System.out.println("Seller has got a bread");
            System.out.println("Bread quantity is: " + storageSeller);
            if (storageSeller == 5) {
                isSelling = false;
                isBuying = true;
                notifyAll();
            }
        }
    }

    public synchronized void buyBread() {
        while (!isBuying) {
            try {
                wait();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        storageSeller--;
        System.out.println("Buyer is buying bread");
        try {
            Thread.sleep(1000);
            System.out.println("Buyer ate bread ");
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        System.out.println("Buyer ate bread: " + (5 - storageSeller));
        if (storageSeller == 0) {
            isBuying = false;
            isSelling = true;
            notifyAll();
        }
    }
}

class Producer implements Runnable {
    Market market;

    public Producer(Market market) {
        this.market = market;
    }

    @Override
    public void run() {
        for (int i = 0; i < 10; i++) {
            System.out.println("producer iteration " + i);
            market.produceBread();
        }
    }
}

class Seller implements Runnable {
    Market market;

    public Seller(Market market) {
        this.market = market;
    }

    @Override
    public void run() {
        for (int i = 0; i < 11; i++) {
            System.out.println("seller iteration " + i);
            market.sellBread();
        }
    }
}

class Buyer implements Runnable {
    Market market;

    public Buyer(Market market) {
        this.market = market;
    }

    @Override
    public void run() {
        for (int i = 0; i < 10; i++) {
            System.out.println("buyer iteration " + i);
            market.buyBread();
        }
    }
}

