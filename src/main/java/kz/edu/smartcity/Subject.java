package kz.edu.smartcity;

public interface Subject<E> {
    void subscribe(String eventType, Observer<E> observer);
    void unsubscribe(String eventType, Observer<E> observer);
    void publish(E event);
}
