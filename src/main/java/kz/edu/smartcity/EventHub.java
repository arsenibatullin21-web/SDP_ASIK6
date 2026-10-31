package kz.edu.smartcity;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public final class EventHub implements Subject<CityEvent> {
    private final ConcurrentHashMap<String, CopyOnWriteArrayList<Observer<CityEvent>>> observers =
            new ConcurrentHashMap<>();

    @Override
    public void subscribe(String eventType, Observer<CityEvent> observer) {
        CopyOnWriteArrayList<Observer<CityEvent>> list = observers.get(eventType);
        if (list == null) {
            CopyOnWriteArrayList<Observer<CityEvent>> newList = new CopyOnWriteArrayList<>();
            CopyOnWriteArrayList<Observer<CityEvent>> oldList = observers.putIfAbsent(eventType, newList);
            list = oldList == null ? newList : oldList;
        }
        list.addIfAbsent(observer);
    }

    @Override
    public void unsubscribe(String eventType, Observer<CityEvent> observer) {
        CopyOnWriteArrayList<Observer<CityEvent>> list = observers.get(eventType);
        if (list != null) {
            list.remove(observer);
        }
    }

    @Override
    public void publish(CityEvent event) {
        CopyOnWriteArrayList<Observer<CityEvent>> list = observers.get(event.getType());
        if (list != null) {
            for (Observer<CityEvent> observer : list) {
                observer.update(event);
            }
        }
    }
}
