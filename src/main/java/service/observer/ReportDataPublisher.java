package service.observer;

import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class ReportDataPublisher {
    private static final Logger LOG = Logger.getLogger(ReportDataPublisher.class.getName());

    private final CopyOnWriteArrayList<ReportDataObserver> observers = new CopyOnWriteArrayList<>();

    public void subscribe(ReportDataObserver observer) {
        if (observer != null) {
            observers.addIfAbsent(observer);  
        }
    }

    public void unsubscribe(ReportDataObserver observer) {
        observers.remove(observer);
    }

    public void notifyOrderPaid() {
        for (ReportDataObserver o : observers) {
            try {
                o.onOrderPaid();
            } catch (RuntimeException e) {
                LOG.log(Level.WARNING, "Observer bị lỗi khi xử lý onOrderPaid", e);
            }
        }
    }
}
