package perf.qed.internal.executor;

import perf.qed.internal.action.Action;

import java.util.Collection;
import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * Created by wreicher
 */
public class ActionQueue implements BlockingDeque<Action>{


    private Deque<Action> actions;
    private Semaphore lock;

    public ActionQueue(){
        actions = new LinkedList<>();
        lock = new Semaphore(1);
    }

    @Override
    public void addFirst(Action action) {
        actions.addFirst(action);
    }

    @Override
    public void addLast(Action action) {
        actions.addLast(action);
    }

    @Override
    public boolean offerFirst(Action action) {
        actions.offerFirst(action);
        return true;
    }

    @Override
    public boolean offerLast(Action action) {
        actions.offerLast(action);
        return true;
    }

    @Override
    public Action removeFirst() {
        return actions.removeFirst();
    }

    @Override
    public Action removeLast() {
        return actions.removeLast();
    }

    @Override
    public Action pollFirst() {
        return actions.pollFirst();
    }

    @Override
    public Action pollLast() {
        return actions.pollLast();
    }

    @Override
    public Action getFirst() {
        return actions.getFirst();
    }

    @Override
    public Action getLast() {
        return actions.getLast();
    }

    @Override
    public Action peekFirst() {
        return actions.peekFirst();
    }

    @Override
    public Action peekLast() {
        return actions.peekLast();
    }

    @Override
    public void putFirst(Action action) throws InterruptedException {
        actions.addFirst(action);
    }

    @Override
    public void putLast(Action action) throws InterruptedException {
        actions.addLast(action);
    }

    @Override
    public boolean offerFirst(Action action, long timeout, TimeUnit unit) throws InterruptedException {
        return actions.offerFirst(action);
    }

    @Override
    public boolean offerLast(Action action, long timeout, TimeUnit unit) throws InterruptedException {
        return actions.offerLast(action);
    }

    @Override
    public Action takeFirst() throws InterruptedException {
        return actions.pollFirst();
    }

    @Override
    public Action takeLast() throws InterruptedException {
        return actions.pollLast();
    }

    @Override
    public Action pollFirst(long timeout, TimeUnit unit) throws InterruptedException {
        return actions.pollFirst();
    }

    @Override
    public Action pollLast(long timeout, TimeUnit unit) throws InterruptedException {
        return actions.pollLast();
    }

    @Override
    public boolean removeFirstOccurrence(Object o) {
        return actions.removeFirstOccurrence(o);
    }

    @Override
    public boolean removeLastOccurrence(Object o) {
        return actions.removeLastOccurrence(o);
    }

    @Override
    public boolean add(Action action) {
        actions.add(action);
        return true;
    }

    @Override
    public boolean offer(Action action) {
        actions.offer(action);
        return true;
    }

    @Override
    public Action remove() {
        if(actions.isEmpty()){
            return null;
        }
        return actions.remove();
    }

    @Override
    public Action poll() {
        return actions.poll();
    }

    @Override
    public Action element() {
        return actions.element();
    }

    @Override
    public Action peek() {
        return actions.peek();
    }

    @Override
    public void put(Action action) throws InterruptedException {
        actions.addLast(action);
    }

    @Override
    public boolean offer(Action action, long timeout, TimeUnit unit) throws InterruptedException {
        return false;
    }

    @Override
    public Action take() throws InterruptedException {
        return actions.pollFirst();
    }

    @Override
    public Action poll(long timeout, TimeUnit unit) throws InterruptedException {
        return actions.poll();
    }

    @Override
    public int remainingCapacity() {
        return Integer.MAX_VALUE;
    }

    @Override
    public boolean remove(Object o) {
        return actions.remove(o);
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        return actions.containsAll(c);
    }

    @Override
    public boolean addAll(Collection<? extends Action> c) {
        return actions.addAll(c);
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        return actions.removeAll(c);
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        return actions.retainAll(c);
    }

    @Override
    public void clear() {
        actions.clear();
    }

    @Override
    public int size() {
        return actions.size();
    }

    @Override
    public boolean isEmpty() {
        return actions.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
        return actions.contains(o);
    }

    @Override
    public Iterator<Action> iterator() {
        return actions.iterator();
    }

    @Override
    public Iterator<Action> descendingIterator() {
        return actions.descendingIterator();
    }

    @Override
    public void push(Action action) {
        actions.push(action);
    }

    @Override
    public Action pop() {
        return actions.pop();
    }

    @Override
    public Object[] toArray() {
        return actions.toArray();
    }

    @Override
    public <T> T[] toArray(T[] a) {
        return actions.toArray(a);
    }

    @Override
    public int drainTo(Collection<? super Action> c) {
        return 0;
    }

    @Override
    public int drainTo(Collection<? super Action> c, int maxElements) {
        return 0;
    }

}
