package simple2;

import lombok.Getter;
import lombok.Setter;
import lombok.Synchronized;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/*
 ******* Producer ********
 * все время производит хлеб
 * с заданной производительностью (sleep).
 ****** Delivery *********
 * перевозит хлеб со склада * Producer * на склад * Seller *
 * перевозчику задается вместительность хлеба и скорость перевозки, а также * Producer * и * Seller *
 * если у * Producer * недостаточно хлеба, ждет (sleep)
 * работает два перевозчика (2 объекта, 2 потока)
 * !!!! не получилось !!!!! после доставки * Delivery * должен wait, пока у * Seller * не возникнет надобность в поставке, и он не разбудит notifyAll()
 * ****** Seller **********
 * получает хлеб от * Delivery * (используя вспомогательный склад)
 * продает
 * !!!! не получилось !!!!! если количество на складе меньше заданного, вызывает перевозчиков notifyAll()
 * ****** Buyer *********
 * Стоит в очереди , покупает случайное количество (1-4) у * Seller *, ест, если хлеба недостаточно, уходит без хлеба
 * покупают 3 потока через ExecutorService
 * ******* ThreadInterrupt ******
 * регулирует время процесса
 * по истечении времени прерывает все потоки, кроме * Buyer *
 */

public class Bread2 {
    public static void main(String[] args) {
        int timeWorking = 10000;

        Producer producer = new Producer(100);
        Seller seller = new Seller(10);
        Delivery delivery1 = new Delivery(producer, seller, 3, 1000);
        Delivery delivery2 = new Delivery(producer, seller, 5, 1500);

        List<Thread> listThread = new ArrayList<>();

        Thread threadProducer = new Thread(producer);
        listThread.add(threadProducer);
        Thread threadSeller = new Thread(seller);
        listThread.add(threadSeller);
        Thread threadDelivery1 = new Thread(delivery1);
        listThread.add(threadDelivery1);
        Thread threadDelivery2 = new Thread(delivery2);
        listThread.add(threadDelivery2);

        ThreadInterrupt threadInterrupt = new ThreadInterrupt(timeWorking, listThread);

        threadProducer.setName("Thread--Producer");
        threadSeller.setName("Thread--Seller");
        threadDelivery1.setName("Thread--Delivery-1");
        threadDelivery2.setName("Thread--Delivery-2");

        threadProducer.start();
        threadSeller.start();
        threadDelivery1.start();
        threadDelivery2.start();

        threadInterrupt.start();

        int nThread = 2;
        String threadName = "Pool - Buyer - ";

        ExecutorService executorService = Executors.newFixedThreadPool(nThread);
        for (int n = 0; n < nThread; n++) {
            int finalN = n + 1;
            executorService.submit(() -> Thread.currentThread().setName(threadName + String.valueOf(finalN)));
        }
        long start = System.currentTimeMillis();
        while (System.currentTimeMillis() - start < timeWorking) {
            executorService.execute(new Buyer(seller));
        }
        executorService.shutdownNow();
    }
}

//Класс, который задает время работы программы, после все потоки прерываются
class ThreadInterrupt extends Thread {
    int workingTime;
    List<Thread> list;

    public ThreadInterrupt(int workingTime, List<Thread> list) {
        this.workingTime = workingTime;
        this.list = list;
    }

    public void run() {
        try {
            Thread.sleep(workingTime);
            list.forEach(Thread::interrupt);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}

//Класс, который производит хлеб с определенной скоростью и хранит на складе
@Getter
@Setter
class Producer extends Thread implements Runnable {
    private int storageProducer;
    private int productionSpeed;

    public Producer(int productionSpeed) {
        this.productionSpeed = productionSpeed;
        this.storageProducer = 10;
    }

    @Override
    public void run() {
        while (!isInterrupted()) {
            storageProducer++;
            System.out.println("Producer has " + storageProducer + " bread on the storage --> thread : " + currentThread().getName());
            try {
                sleep(productionSpeed);
            } catch (InterruptedException e) {
                System.out.println("Production was interrupted");
                return;
            }
        }
    }
}

//Класс, который перевозит хлеб, определенное количество
class Delivery extends Thread implements Runnable {
    Producer producer;
    Seller seller;
    private int capacity;
    private int deliverySpeed;
    int countOfDeliveries;
//    public static final Object obj = new Object();

    public Delivery(Producer producer, Seller seller, int capacity, int deliverySpeed) {
        this.capacity = capacity;
        this.deliverySpeed = deliverySpeed;
        this.producer = producer;
        this.seller = seller;
        this.countOfDeliveries = 0;
    }

    @Override
    synchronized public void run() {
        System.out.println(currentThread().getState());
        while (!isInterrupted()) {
            if (producer.getStorageProducer() >= capacity) {
                producer.setStorageProducer(producer.getStorageProducer() - capacity);
                countOfDeliveries++;
                System.out.println("Delivery has " + capacity + " bread in the car, delivery #" + countOfDeliveries + " --> thread : " + currentThread().getName());
//                synchronized (Delivery.class) {
                try {
                    sleep(deliverySpeed);
                    seller.setStorageSellerDelivery(seller.getStorageSellerDelivery() + capacity);
                    System.out.println("Delivery got " + capacity + " bread to the Seller --> thread : " + currentThread().getName());
//                        wait();
                } catch (InterruptedException e) {
                    System.out.println("Delivery was interrupted");
                    return;
                }
//                }
            } else {
                try {
                    System.out.println("Delivery is waiting when producer make bread");
                    sleep((long) producer.getProductionSpeed() * capacity);
                } catch (InterruptedException e) {
                    System.out.println("Delivery was interrupted");
                    return;
                }
            }
        }
    }
}

@Getter
@Setter
class Seller extends Thread implements Runnable {
    volatile private int storageSeller;
    volatile private int storageSellerDelivery;
    private int breakTime;
    private int minNumberInStorage;

    public Seller(int minNumberInStorage) {
        this.minNumberInStorage = minNumberInStorage;
        this.storageSeller = 10;
        this.storageSellerDelivery = 0;
        this.breakTime = 1000;
    }

    @Override
    public void run() {
        while (!isInterrupted()) {
            storageSeller += storageSellerDelivery;
            storageSellerDelivery = 0;
            System.out.println("Seller has " + storageSeller + " bread on the storage --> thread : " + currentThread().getName());
            try {
                sleep(breakTime / 5);
            } catch (InterruptedException e) {
                System.out.println("Seller was interrupted1");
                return;
            }
            synchronized (Seller.class) {
                if (storageSeller < minNumberInStorage) {
                    try {
//                        notifyAll();
                        System.out.println("Seller is waiting Delivery, count of bread is " + storageSeller);
                        sleep(breakTime);
                    } catch (InterruptedException e) {
                        System.out.println("Seller was interrupted2");
                        return;
                    }
                }
            }
        }
    }
}

class Buyer extends Thread implements Runnable {
    public Random random = new Random();
    Seller seller;

    public Buyer(Seller seller) {
        this.seller = seller;
    }

    @Override
    public void run() {
        try {
            sleep(200);
            System.out.println("Buyer is waiting in line --> Thread : " + currentThread().getName());
        } catch (InterruptedException e) {
            System.out.println("Buyer was interrupted  --> Thread : " + currentThread().getName());
            return;
        }
        synchronized (Buyer.class) {
            int countBread = random.nextInt(1, 5);
            if (countBread <= seller.getStorageSeller()) {
                seller.setStorageSeller(seller.getStorageSeller() - countBread);
                System.out.println("Buyer bought " + countBread + " bread, rest bread " + seller.getStorageSeller() + " --> Thread : " + currentThread().getName());
                try {
                    sleep(100);
                    System.out.println("Buyer went eating bread --> Thread : " + currentThread().getName());
                } catch (InterruptedException e) {
                    System.out.println("Buyer was interrupted  --> Thread : " + currentThread().getName());
                    return;
                }
            } else {
                try {
                    sleep(200);
                    System.out.println("Buyer did not buy bread and went away, wanted " + countBread + " seller had " + seller.getStorageSeller() + " --> Thread : " + currentThread().getName());
                } catch (InterruptedException e) {
                    System.out.println("Buyer was interrupted  --> Thread : " + currentThread().getName());
                    return;
                }
            }
        }
    }
}

