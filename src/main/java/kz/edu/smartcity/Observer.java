package kz.edu.smartcity;

public interface Observer<E> {
    void update(E event);
}
