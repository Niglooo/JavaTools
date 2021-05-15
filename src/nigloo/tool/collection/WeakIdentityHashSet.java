package nigloo.tool.collection;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.AbstractSet;
import java.util.Arrays;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;

/**
 * Set of WeakReference which uses referential equality (a == b instead of a.equals(b)) 
 *
 * @param <T> Type of elements
 */
public class WeakIdentityHashSet<T> extends AbstractSet<T> {

	private static final int DEFAULT_INITIAL_CAPACITY = 10;
	private static final float DEFAULT_LOAD_FACTOR = 0.75f;
	
	private final ReferenceQueue<T> queue = new ReferenceQueue<>();
	
	private Node<T>[] table;
	private final float loadFactor;
	
	private int size;
	
	private int modCount;
	
	public WeakIdentityHashSet() {
		this.loadFactor = DEFAULT_LOAD_FACTOR; // must be set BEFORE use of computeCapacity
		@SuppressWarnings({ "unchecked" })
		Node<T>[] t = (Node<T>[]) new Node<?>[computeCapacity(DEFAULT_INITIAL_CAPACITY)];
		this.table = t;
		this.size = 0;
	}

	@Override
	public int size() {
		if (size == 0)
			return 0;
		
		reap();
		return size;
	}

	@Override
	public boolean isEmpty() {
		return size() == 0;
	}

	@Override
	public boolean contains(Object o) {
		return o == null ? false : getNode(o) != null;
	}

	@Override
	public Iterator<T> iterator() {
		return new HashIterator();
	}
	
	private class HashIterator implements Iterator<T> {

		private int nextIndex;
		private Node<T> nextNode;

		/**
		 * Strong reference needed to avoid disappearance of value
		 * between hasNext and next
		 */
		private T nextValue;

		/**
		 * Used by HashIterator.remove
		 */
		private Node<T> currentNode;
		
		int expectedModCount;
		
		
		private HashIterator() {
			expectedModCount = modCount;
			
			currentNode = null;
			nextValue = null;
			for (nextIndex = 0 ; nextIndex < table.length ; nextIndex++) {
				if (table[nextIndex] != null && (nextValue = table[nextIndex].get()) != null) {
					nextNode = table[nextIndex];
					return;
				}
			}
		}
		
		@Override
		public boolean hasNext() {
			return nextValue != null;
		}

		@Override
		public T next() {
			if (modCount != expectedModCount)
				throw new ConcurrentModificationException();
			if (nextValue == null)
				throw new NoSuchElementException();
			
			T value = nextValue;
			currentNode = nextNode;
			
			nextNode = nextNode.next;
			while (nextIndex < table.length)
			{
				while (nextNode != null)
				{
					nextValue = nextNode.get();
					if (nextValue != null)
						return value;
					
					nextNode = nextNode.next;
				}
				
				nextIndex++;
				if (nextIndex < table.length)
					nextNode = table[nextIndex];
				else
					nextValue = null;
			}
			
			return value;
		}
		
		@Override
		public void remove() {
			if (modCount != expectedModCount)
				throw new ConcurrentModificationException();
			if (currentNode == null)
				throw new IllegalStateException();

			modCount++;
			currentNode.enqueue();
			currentNode = null;
			
			expectedModCount = modCount;
		}
	}

	@Override
	public Object[] toArray() {
		return super.toArray();
	}

	@Override
	public <T2> T2[] toArray(T2[] a) {
		return super.toArray(a);
	}

	@Override
	public boolean add(T e) {
		if (e == null)
			throw new NullPointerException();
		
		if (getNode(e) == null) {
			ensureCapacity(size + 1);
			int hash = hash(e);
			int index = index(hash, table.length);
			Node<T> newNode = new Node<T>(e, queue, table[index]);
			table[index] = newNode;
			size++;
			modCount++;
			return false;
		}
		
		return true;
	}

	@Override
	public boolean remove(Object o) {
		if (o == null)
			throw new NullPointerException();
		
		Node<T> node = getNode(o);
		if (node == null)
			return false;
		
		node.enqueue();
		modCount++;
		
		return true;
	}

	@Override
	public boolean containsAll(Collection<?> c) {
		for (Object e : c)
			if (!contains(e))
				return false;
		
		return true;
	}

	@Override
	public boolean addAll(Collection<? extends T> c) {
		boolean changed = false;
		for (T e : c)
			if (add(e))
				changed = true;
		
		return changed;
	}

	@Override
	public boolean retainAll(Collection<?> c) {
		return super.retainAll(c);
	}

	@Override
	public boolean removeAll(Collection<?> c) {
		
		boolean changed = false;
		for (Object e : c) {
			if (e == null)
				throw new NullPointerException();
		
			Node<T> node = getNode(e);
			if (node != null) {
				node.enqueue();
				modCount++;
				changed = true;
			}
		}
		reap();
		
		return changed;
	}

	@Override
	public void clear() {
		
		while (queue.poll() != null);
		
		Arrays.fill(table, null);
		size = 0;
		modCount++;

		while (queue.poll() != null);
	}
	
	private Node<T> getNode(Object value) {
		int index = index(hash(value), table.length);
		
		for (Node<T> node = table[index] ; node != null ; node = node.next)
			if (node.get() == value)// TODO use node.refersTo(value) instead in java 16
				return node;
		
		return null;
	}
	
	private void ensureCapacity(int size) {
		
		int capacity = computeCapacity(size);
		
		if (capacity > table.length)
			reHash(capacity);
	}
	
	private void reHash(int newCapacity) {
		
		reap();
		
		@SuppressWarnings("unchecked")
		Node<T>[] newTable = (Node<T>[]) new Node<?>[newCapacity];
		
		for (Node<T> node : table) {
			for (Node<T> current = node, next ; current != null ; current = next) {
				next = current.next;
				int newIndex = index(current.hash, newTable.length);
				// Insert at the head of the chain
				current.next = newTable[newIndex];
				newTable[newIndex] = current;
			}
		}
		
		table = newTable;
	}
	
	private void reap() {
		
		Object x;
		while ((x = queue.poll()) != null) {
			synchronized (queue) {
				@SuppressWarnings("unchecked")
				Node<T> toRemove = (Node<T>) x;
				int index = index(toRemove.hash, table.length);
				Node<T> prev = null;
				Node<T> current = table[index];
				
				while (current != null) {
					if (current == toRemove) {
						if (prev == null) {
							table[index] = current.next;
						}
						else {
							prev.next = current.next;
						}
						size--;
						break;
					}
					prev = current;
					current = current.next;
				}
			}
		}
	}
	
	private static class Node<T> extends WeakReference<T>
	{
		final int hash;
		Node<T> next;
		
		Node (T value, ReferenceQueue<T> queue, Node<T> next) {
			super(value, queue);
			this.hash = hash(value);
			this.next = next;
		}
		
		@Override
		public String toString() {
			return Objects.toString(get());
		}
	}
	
	

	/**
	 * Computes o.hashCode() and spreads (XORs) higher bits of hash to lower.
	 * Because the table uses power-of-two masking, sets of hashes that vary only in
	 * bits above the current mask will always collide. (Among known examples are
	 * sets of Float keys holding consecutive whole numbers in small tables.) So we
	 * apply a transform that spreads the impact of higher bits downward. There is a
	 * tradeoff between speed, utility, and quality of bit-spreading. Because many
	 * common sets of hashes are already reasonably distributed (so don't benefit
	 * from spreading), we just XOR some shifted bits in the cheapest possible way
	 * to reduce systematic lossage, as well as to incorporate impact of the highest
	 * bits that would otherwise never be used in index calculations because of
	 * table bounds.
	 */
	private static int hash(Object o) {
		int h;
		return (o == null) ? 0 : (h = o.hashCode()) ^ (h >>> 16);
	}
	
	private static int index(int hash, int capacity) {
		return (capacity - 1) & hash;
	}
	
	private int computeCapacity(int size) {
		int c = 1;
		while (c * loadFactor < size)
			c <<= 1;
		
		return c;
	}
	
	/*
	public void print() {
		int notNull = 0;
		int withNull = 0;
		
		int i=0;
		for (Node<T> node : table) {
			System.out.print("["+i+"] =>");
			for (Node<T> current = node ; current != null ; current = current.next)
			{
				Object value = current.get();
				withNull++;
				if (value != null)
					notNull++;
				
				System.out.print(" "+value);
			}
			i++;
			System.out.println();
		}
		System.out.println("Size: "+size);
		System.out.println(notNull+" / "+withNull);
	}
	*/
}
