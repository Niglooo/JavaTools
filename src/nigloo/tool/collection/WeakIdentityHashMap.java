package nigloo.tool.collection;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.AbstractCollection;
import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
import java.util.Spliterator;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Map with WeakReference keys which uses referential equality (a == b instead of a.equals(b)) 
 *
 * @param <K> Type of elements
 */
public class WeakIdentityHashMap<K, V> implements Map<K, V> {

	private static final int DEFAULT_INITIAL_CAPACITY = 10;
	private static final float DEFAULT_LOAD_FACTOR = 0.75f;
	
	private final ReferenceQueue<K> queue = new ReferenceQueue<>();
	
	private Node<K,V>[] table;
	private final float loadFactor;
	
	private int size;
	
	private int modCount;
	
	
	private transient Set<K> keySet;
	private transient Collection<V> values;
	private transient Set<Entry<K,V>> entrySet;
	
	public WeakIdentityHashMap() {
		this.loadFactor = DEFAULT_LOAD_FACTOR; // must be set BEFORE use of computeCapacity
		@SuppressWarnings({ "unchecked" })
		Node<K,V>[] t = (Node<K,V>[]) new Node<?,?>[computeCapacity(DEFAULT_INITIAL_CAPACITY)];
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
	public boolean containsKey(Object key) {
		return key == null ? false : getNode(key, false) != null;
	}
	
	@Override
	public V get(Object key) {
		Node<K,V> node = getNode(key, false);
		return node != null ? node.value : null;
	}
	
	@Override
	public V getOrDefault(Object key, V defaultValue) {
		Node<K,V> node = getNode(key, false);
		return node != null ? node.value : defaultValue;
	}
	
	@Override
	public boolean containsValue(Object value)
	{
		ValueIterator it = new ValueIterator();
		while (it.hasNext())
			if (Objects.equals(it.next(), value))
				return true;
		
		return false;
	}
	
	@Override
	public V put(K key, V value)
	{
		if (key == null)
			throw new NullPointerException();
		
		return getNode(key, true).setValue(value);
	}
	
	@Override
	public void putAll(Map<? extends K, ? extends V> m)
	{
		int numKeysToBeAdded = m.size();
		if (numKeysToBeAdded == 0)
			return;
		
		if (numKeysToBeAdded > table.length * loadFactor)
			ensureCapacity((int) (numKeysToBeAdded / loadFactor + 1));
		
		for (Entry<? extends K, ? extends V> e : m.entrySet())
			put(e.getKey(), e.getValue());
	}

	@Override
	public V remove(Object key) {
		if (key == null)
			throw new NullPointerException();
		
		Node<K,V> node = getNode(key, false);
		if (node == null)
			return null;
		
		node.enqueue();
		modCount++;
		
		return node.value;
	}
	
	@Override
	public boolean remove(Object key, Object value) {
		if (key == null)
			throw new NullPointerException();
		
		Node<K,V> node = getNode(key, false);
		if (node == null || !Objects.equals(node.value, value))
			return false;
		
		node.enqueue();
		modCount++;
		return true;
	}

	@Override
	public void clear() {
		
		Arrays.fill(table, null);
		size = 0;
		modCount++;

		while (queue.poll() != null);
	}
	
	@Override
	public V computeIfAbsent(K key, Function<? super K, ? extends V> mappingFunction)
	{
		int hash = hash(key);
		int index = index(hash, table.length);
		Node<K,V> node;
		
		for (node = table[index] ; node != null ; node = node.next)
			if (node.refersTo((K) key))
				break;
		
		if (node != null && node.value != null)
			return node.value;
		
		V newValue = mappingFunction.apply(key);
		
		if (newValue == null)
			return null;
		
		if (node == null) {
			ensureCapacity(size+1);
			index = index(hash, table.length);
			node = new Node<K, V>(hash, (K) key, newValue, queue, table[index]);
			table[index] = node;
			size++;
			modCount++;
		}
		else {
			node.setValue(newValue);
		}
		
		return newValue;
		
	}
	
	@Override
	public void forEach(BiConsumer<? super K, ? super V> action) {
		Objects.requireNonNull(action);
		int expectedModCount = modCount;
	
		EntryIterator it = new EntryIterator();
		while (it.hasNext()) {
			Entry<K, V> entry = it.next();
			K key = entry.getKey();// Prevent key from disappearing during action.accept
			if (key == null)
				continue;
			
			action.accept(key, entry.getValue());
			
			if (expectedModCount != modCount)
				throw new ConcurrentModificationException();
		}
	}

	@Override
	public void replaceAll(BiFunction<? super K, ? super V, ? extends V> function) {
		Objects.requireNonNull(function);
		int expectedModCount = modCount;
	
		EntryIterator it = new EntryIterator();
		while (it.hasNext()) {
			Entry<K, V> entry = it.next();
			K key = entry.getKey();// Prevent key from disappearing just before the call to action.accept
			if (key == null)
				continue;
			
			entry.setValue(function.apply(key, entry.getValue()));
			
			if (expectedModCount != modCount)
				throw new ConcurrentModificationException();
		}
	}

	@Override
	public Set<K> keySet() {
		return keySet != null ? keySet : (keySet = new KeySet());
	}
	
	@Override
	public Collection<V> values() {
		return values != null ? values : (values = new Values());
	}
	
	@Override
	public Set<Entry<K, V>> entrySet() {
		return entrySet != null ? entrySet : (entrySet = new EntrySet());
	}

	@SuppressWarnings("unchecked")
	private Node<K,V> getNode(Object key, boolean createIfAbsent) {
		int hash = hash(key);
		int index = index(hash, table.length);
		
		for (Node<K,V> node = table[index] ; node != null ; node = node.next)
			if (node.refersTo((K) key))
				return node;
		
		if (createIfAbsent) {
			ensureCapacity(size+1);
			index = index(hash, table.length);
			Node<K,V> node = new Node<K, V>(hash, (K) key, null, queue, table[index]);
			table[index] = node;
			size++;
			modCount++;
			return node;
		}
		else {
			return null;
		}
	}
	
	private void ensureCapacity(int size) {
		
		int capacity = computeCapacity(size);
		
		if (capacity > table.length)
			reHash(capacity);
	}
	
	private void reHash(int newCapacity) {
		
		reap();
		
		@SuppressWarnings("unchecked")
		Node<K,V>[] newTable = (Node<K,V>[]) new Node<?,?>[newCapacity];
		
		for (Node<K,V> node : table) {
			for (Node<K,V> current = node, next ; current != null ; current = next) {
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
				Node<K,V> toRemove = (Node<K,V>) x;
				int index = index(toRemove.hash, table.length);
				Node<K,V> prev = null;
				Node<K,V> current = table[index];
				
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
	
	private static class Node<K, V> extends WeakReference<K> implements Entry<K, V>
	{
		final int hash;
		V value;
		Node<K, V> next;
		
		Node (int hash, K key, V value, ReferenceQueue<K> queue, Node<K, V> next) {
			super(key, queue);
			this.hash = hash;
			this.value = value;
			this.next = next;
		}
		
		@Override
		public int hashCode()
		{
			return super.hashCode() ^ Objects.hashCode(value);
		}
		
		@Override
		public boolean equals(Object obj)
		{
			if (obj == this)
				return true;
			if (!(obj instanceof Entry<?,?> other)) {
				return false;
			}
			else {
				@SuppressWarnings("unchecked")
				K otherKey = (K) other.getKey();
				return refersTo(otherKey) && !refersTo(null) && Objects.equals(value, other.getValue());
			}
		}
		
		@Override
		public K getKey()
		{
			return get();
		}

		@Override
		public V getValue()
		{
			return value;
		}

		@Override
		public V setValue(V value)
		{
			V old = this.value;
			this.value = value;
			return old;
		}
		
		@Override
		public String toString() {
			return "{"+get()+" -> "+value+"}";
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
		return (h = System.identityHashCode(o)) ^ (h >>> 16);
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
	
	private class HashIterator {

		private int nextIndex;
		private Node<K,V> nextNode;

		/**
		 * Strong reference needed to avoid disappearance of value
		 * between hasNext and next
		 */
		private K nextKey;

		/**
		 * Used by HashIterator.remove
		 */
		private Node<K,V> currentNode;
		
		int expectedModCount;
		
		
		private HashIterator() {
			expectedModCount = modCount;
			
			currentNode = null;
			nextKey = null;
			for (nextIndex = 0 ; nextIndex < table.length ; nextIndex++) {
				if (table[nextIndex] != null && (nextKey = table[nextIndex].get()) != null) {
					nextNode = table[nextIndex];
					return;
				}
			}
		}
		
		public final boolean hasNext() {
			return nextKey != null;
		}
		
		public final Node<K, V> nextNode() {
			if (modCount != expectedModCount)
				throw new ConcurrentModificationException();
			if (nextKey == null)
				throw new NoSuchElementException();
			
			currentNode = nextNode;
			
			nextNode = nextNode.next;
			while (nextIndex < table.length)
			{
				while (nextNode != null)
				{
					nextKey = nextNode.get();
					if (nextKey != null)
						return currentNode;
					
					nextNode = nextNode.next;
				}
				
				nextIndex++;
				if (nextIndex < table.length)
					nextNode = table[nextIndex];
				else
					nextKey = null;
			}
			
			return currentNode;
		}
		
		public final void remove() {
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
	
	private class KeyIterator extends HashIterator implements Iterator<K> {
		public final K next() {
			return nextNode().get();
		}
	}
	
	private class ValueIterator extends HashIterator implements Iterator<V> {
		public final V next() {
			return nextNode().value;
		}
	}
	
	private class EntryIterator extends HashIterator implements Iterator<Entry<K, V>> {
		public final Entry<K, V> next() {
			return nextNode();
		}
	}
	
	private class KeySet extends AbstractSet<K> {
		public Iterator<K> iterator() {
			return new KeyIterator();
		}

		public int size() {
			return WeakIdentityHashMap.this.size();
		}

		public boolean contains(Object o) {
			return containsKey(o);
		}

		public boolean remove(Object o) {
			if (containsKey(o)) {
				WeakIdentityHashMap.this.remove(o);
				return true;
			}
			else
				return false;
		}

		public void clear() {
			WeakIdentityHashMap.this.clear();
		}

		public Spliterator<K> spliterator() {
			return new KeySpliterator<>(WeakIdentityHashMap.this, 0, -1, 0, 0);
		}
	}

	private class Values extends AbstractCollection<V> {
		public Iterator<V> iterator() {
			return new ValueIterator();
		}

		public int size() {
			return WeakIdentityHashMap.this.size();
		}

		public boolean contains(Object o) {
			return containsValue(o);
		}

		public void clear() {
			WeakIdentityHashMap.this.clear();
		}

		public Spliterator<V> spliterator() {
			return new ValueSpliterator<>(WeakIdentityHashMap.this, 0, -1, 0, 0);
		}
	}

	private class EntrySet extends AbstractSet<Entry<K,V>> {
		public Iterator<Entry<K,V>> iterator() {
			return new EntryIterator();
		}

		public boolean contains(Object o) {
			Node<K, V> node;
			return o instanceof Entry<?, ?> e
					&& (node = getNode(e.getKey(), false)) != null
					&& node.equals(e);
		}

		public boolean remove(Object o) {
			Node<K, V> node;
			if(o instanceof Entry<?, ?> e
				&& (node = getNode(e.getKey(), false)) != null
				&& node.equals(e))
			{
				node.enqueue();
				return true;
			}
			
			return false;
		}

		public int size() {
			return WeakIdentityHashMap.this.size();
		}

		public void clear() {
			WeakIdentityHashMap.this.clear();
		}

		private List<Entry<K,V>> deepCopy() {
			List<Entry<K,V>> list = new ArrayList<>(size());
			for (Map.Entry<K,V> e : this)
				list.add(new AbstractMap.SimpleEntry<>(e));
			return list;
		}

		public Object[] toArray() {
			return deepCopy().toArray();
		}

		public <T> T[] toArray(T[] a) {
			return deepCopy().toArray(a);
		}

		public Spliterator<Entry<K,V>> spliterator() {
			return new EntrySpliterator<>(WeakIdentityHashMap.this, 0, -1, 0, 0);
		}
	}
	
	/**
	 * Similar form as other hash Spliterators, but skips dead
	 * elements.
	 */
	static class WeakHashMapSpliterator<K,V> {
		final WeakIdentityHashMap<K,V> map;
		Node<K,V> current;    // current node
		int index;            // current index, modified on advance/split
		int fence;            // -1 until first use; then one past last index
		int est;              // size estimate
		int expectedModCount; // for comodification checks

		WeakHashMapSpliterator(WeakIdentityHashMap<K,V> m, int origin,
							   int fence, int est,
							   int expectedModCount) {
			this.map = m;
			this.index = origin;
			this.fence = fence;
			this.est = est;
			this.expectedModCount = expectedModCount;
		}

		final int getFence() { // initialize fence and size on first use
			int hi;
			if ((hi = fence) < 0) {
				WeakIdentityHashMap<K,V> m = map;
				est = m.size();
				expectedModCount = m.modCount;
				hi = fence = m.table.length;
			}
			return hi;
		}

		public final long estimateSize() {
			getFence(); // force init
			return (long) est;
		}
	}

	static final class KeySpliterator<K,V>
		extends WeakHashMapSpliterator<K,V>
		implements Spliterator<K> {
		KeySpliterator(WeakIdentityHashMap<K,V> m, int origin, int fence, int est,
					   int expectedModCount) {
			super(m, origin, fence, est, expectedModCount);
		}

		public KeySpliterator<K,V> trySplit() {
			int hi = getFence(), lo = index, mid = (lo + hi) >>> 1;
			return (lo >= mid) ? null :
				new KeySpliterator<>(map, lo, index = mid, est >>>= 1,
									 expectedModCount);
		}

		public void forEachRemaining(Consumer<? super K> action) {
			int i, hi, mc;
			if (action == null)
				throw new NullPointerException();
			WeakIdentityHashMap<K,V> m = map;
			Node<K,V>[] tab = m.table;
			if ((hi = fence) < 0) {
				mc = expectedModCount = m.modCount;
				hi = fence = tab.length;
			}
			else
				mc = expectedModCount;
			if (tab.length >= hi && (i = index) >= 0 &&
				(i < (index = hi) || current != null)) {
				Node<K,V> p = current;
				current = null; // exhaust
				do {
					if (p == null)
						p = tab[i++];
					else {
						K x = p.get();
						p = p.next;
						if (x != null) {
							action.accept(x);
						}
					}
				} while (p != null || i < hi);
			}
			if (m.modCount != mc)
				throw new ConcurrentModificationException();
		}

		public boolean tryAdvance(Consumer<? super K> action) {
			int hi;
			if (action == null)
				throw new NullPointerException();
			Node<K,V>[] tab = map.table;
			if (tab.length >= (hi = getFence()) && index >= 0) {
				while (current != null || index < hi) {
					if (current == null)
						current = tab[index++];
					else {
						K x = current.get();
						current = current.next;
						if (x != null) {
							action.accept(x);
							if (map.modCount != expectedModCount)
								throw new ConcurrentModificationException();
							return true;
						}
					}
				}
			}
			return false;
		}

		public int characteristics() {
			return Spliterator.DISTINCT;
		}
	}

	static final class ValueSpliterator<K,V>
		extends WeakHashMapSpliterator<K,V>
		implements Spliterator<V> {
		ValueSpliterator(WeakIdentityHashMap<K,V> m, int origin, int fence, int est,
						 int expectedModCount) {
			super(m, origin, fence, est, expectedModCount);
		}

		public ValueSpliterator<K,V> trySplit() {
			int hi = getFence(), lo = index, mid = (lo + hi) >>> 1;
			return (lo >= mid) ? null :
				new ValueSpliterator<>(map, lo, index = mid, est >>>= 1,
									   expectedModCount);
		}

		public void forEachRemaining(Consumer<? super V> action) {
			int i, hi, mc;
			if (action == null)
				throw new NullPointerException();
			WeakIdentityHashMap<K,V> m = map;
			Node<K,V>[] tab = m.table;
			if ((hi = fence) < 0) {
				mc = expectedModCount = m.modCount;
				hi = fence = tab.length;
			}
			else
				mc = expectedModCount;
			if (tab.length >= hi && (i = index) >= 0 &&
				(i < (index = hi) || current != null)) {
				Node<K,V> p = current;
				current = null; // exhaust
				do {
					if (p == null)
						p = tab[i++];
					else {
						Object x = p.get();
						V v = p.value;
						p = p.next;
						if (x != null)
							action.accept(v);
					}
				} while (p != null || i < hi);
			}
			if (m.modCount != mc)
				throw new ConcurrentModificationException();
		}

		public boolean tryAdvance(Consumer<? super V> action) {
			int hi;
			if (action == null)
				throw new NullPointerException();
			Node<K,V>[] tab = map.table;
			if (tab.length >= (hi = getFence()) && index >= 0) {
				while (current != null || index < hi) {
					if (current == null)
						current = tab[index++];
					else {
						Object x = current.get();
						V v = current.value;
						current = current.next;
						if (x != null) {
							action.accept(v);
							if (map.modCount != expectedModCount)
								throw new ConcurrentModificationException();
							return true;
						}
					}
				}
			}
			return false;
		}

		public int characteristics() {
			return 0;
		}
	}

	static final class EntrySpliterator<K,V>
		extends WeakHashMapSpliterator<K,V>
		implements Spliterator<Entry<K,V>> {
		EntrySpliterator(WeakIdentityHashMap<K,V> m, int origin, int fence, int est,
					   int expectedModCount) {
			super(m, origin, fence, est, expectedModCount);
		}

		public EntrySpliterator<K,V> trySplit() {
			int hi = getFence(), lo = index, mid = (lo + hi) >>> 1;
			return (lo >= mid) ? null :
				new EntrySpliterator<>(map, lo, index = mid, est >>>= 1,
									   expectedModCount);
		}


		public void forEachRemaining(Consumer<? super Map.Entry<K, V>> action) {
			int i, hi, mc;
			if (action == null)
				throw new NullPointerException();
			WeakIdentityHashMap<K,V> m = map;
			Node<K,V>[] tab = m.table;
			if ((hi = fence) < 0) {
				mc = expectedModCount = m.modCount;
				hi = fence = tab.length;
			}
			else
				mc = expectedModCount;
			if (tab.length >= hi && (i = index) >= 0 &&
				(i < (index = hi) || current != null)) {
				Node<K,V> p = current;
				current = null; // exhaust
				do {
					if (p == null)
						p = tab[i++];
					else {
						K k = p.get();
						V v = p.value;
						p = p.next;
						if (k != null) {
							action.accept(new AbstractMap.SimpleImmutableEntry<>(k, v));
						}
					}
				} while (p != null || i < hi);
			}
			if (m.modCount != mc)
				throw new ConcurrentModificationException();
		}

		public boolean tryAdvance(Consumer<? super Map.Entry<K,V>> action) {
			int hi;
			if (action == null)
				throw new NullPointerException();
			Node<K,V>[] tab = map.table;
			if (tab.length >= (hi = getFence()) && index >= 0) {
				while (current != null || index < hi) {
					if (current == null)
						current = tab[index++];
					else {
						K k = current.get();
						V v = current.value;
						current = current.next;
						if (k != null) {
							action.accept(new AbstractMap.SimpleImmutableEntry<>(k, v));
							if (map.modCount != expectedModCount)
								throw new ConcurrentModificationException();
							return true;
						}
					}
				}
			}
			return false;
		}

		public int characteristics() {
			return Spliterator.DISTINCT;
		}
	}
}
