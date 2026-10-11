package service.observer;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public final class ReportDataPublisher {
    private final List<ReportDataObserver> observers = new CopyOnWriteArrayList<>();

    public void subscribe(ReportDataObserver observer) {
        observers.add(observer);
    }

    public void notifyOrderPaid() {
        observers.forEach(ReportDataObserver::onOrderPaid);
    }
}
