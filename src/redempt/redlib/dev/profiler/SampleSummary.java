package redempt.redlib.dev.profiler;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Represents a summary of profiler data
 * @author Redempt
 */
public class SampleSummary {
	
	private List<Sample> samples = new ArrayList<>();
	private Map<String, List<SampleMethod>> methods = new ConcurrentHashMap<>();
	private SampleMethod root;
	private long end;
	private long start;
	
	private SampleMethod getSampleMethod(StackTraceElement elem, SampleMethod parent) {
		String name = getName(elem);
		List<SampleMethod> methods = this.methods.get(name);
		if (methods == null) {
			methods = Collections.synchronizedList(new ArrayList<>());
			SampleMethod method = new SampleMethod(this, name);
			if (parent != null) {
				parent.addChild(method);
			}
			methods.add(method);
			this.methods.put(name, methods);
			return method;
		}
		synchronized (methods) {
			for (SampleMethod method : methods) {
				if (method.getName().equals(name) && method.parent == parent) {
					return method;
				}
			}
		}
		SampleMethod method = new SampleMethod(this, name);
		if (parent != null) {
			parent.addChild(method);
		}
		methods.add(method);
		return method;
	}
	
	protected SampleSummary(Collection<Sample> samples) {
		this.samples.addAll(samples);
		for (Sample sample : samples) {
			StackTraceElement[] stack = sample.getStackTrace();
			add(stack);
		}
	}
	
	protected SampleSummary() {
		start = System.currentTimeMillis();
	}
	
	protected void add(StackTraceElement[] stack) {
		end = System.currentTimeMillis();
		SampleMethod parent = null;
		int i = stack.length - 1;
		if (root == null) {
			StackTraceElement root = stack[stack.length - 1];
			this.root = getSampleMethod(root, null);
			parent = this.root;
			this.root.increment();
			i--;
		}
		for (; i >= 0; i--) {
			StackTraceElement elem = stack[i];
			SampleMethod method = getSampleMethod(elem, parent);
			method.increment();
			parent = method;
		}
	}
	
	/**
	 * @return A clone of this SampleSummary
	 */
	public SampleSummary clone() {
		SampleSummary clone = new SampleSummary();
		if (root == null) {
			return clone;
		}
		Map<String, List<SampleMethod>> methods = new HashMap<>();
		SampleMethod root = this.root.clone(clone);
		Deque<SampleMethod> deque = new ArrayDeque<>();
		deque.add(root);
		while (deque.size() > 0) {
			SampleMethod method = deque.poll();
			deque.addAll(method.getChildren());
			List<SampleMethod> list = methods.get(method.getName());
			if (list == null) {
				list = new ArrayList<>();
				list.add(method);
				methods.put(method.getName(), list);
				continue;
			}
			list.add(method);
		}
		clone.methods = methods;
		clone.root = root;
		return clone;
	}
	
	/**
	 * @return The time this SampleSummary's data starts at
	 */
	public long getStart() {
		return samples == null ? start : samples.get(0).getTime();
	}
	
	/**
	 * @return The time this SampleSummary's data ends at
	 */
	public long getEnd() {
		return samples == null ? end : samples.get(samples.size() - 1).getTime();
	}
	
	/**
	 * @return The difference between the end and start of this SampleSummary's data
	 */
	public long getDuration() {
		return getEnd() - getStart();
	}
	
	/**
	 * @return The root method, which all other methods were called by some descendent of in this summary
	 */
	public SampleMethod getRoot() {
		return root;
	}
	
	/**
	 * @return A map of method names to the {@link SampleMethod}s associated with them. One name can
	 * have multiple mappings because each SampleMethod represents a method <b>and</b> its relative position
	 * in the stack trace.
	 */
	public Map<String, List<SampleMethod>> getMethodsByName() {
		return methods;
	}
	
	private static String getName(StackTraceElement elem) {
		return elem.getClassName() + '#' + elem.getMethodName();
	}
	
	/**
	 * Represents a method and its specific place in the summary tree. Each SampleMethod can be treated
	 * as a node of a tree.
	 */
	public static class SampleMethod {
		
		private String name;
		private String shortName;
		private long count = 1;
		private int depth = 0;
		protected SampleMethod parent = null;
		private Set<SampleMethod> children = new HashSet<>();
		protected SampleSummary summary;
		
		protected SampleMethod(SampleSummary summary, String name) {
			this.name = name;
			this.summary = summary;
		}
		
		/**
		 * @return The percentage of samples this method was reported in - effectively,
		 * how much of the sample duration was spent running this method
		 */
		public double getPrevalence() {
			return (count / (double) summary.root.count) * 100;
		}
		
		/**
		 * @return The SampleMethod which always called this method, or null if this is the root SampleMethod.
		 */
		public SampleMethod getParent() {
			return parent;
		}
		
		/**
		 * @return The set of children of this SampleMethod - the methods it called
		 */
		public Set<SampleMethod> getChildren() {
			return children;
		}
		
		/**
		 * @return The full name of this method
		 */
		public String getName() {
			return name;
		}
		
		/**
		 * @return The depth of this SampleMethod in the tree - its distance from the root
		 */
		public int getDepth() {
			return depth;
		}
		
		/**
		 * @return The shortened method name, with package name and unneeded lambda data removed
		 */
		public String getShortName() {
			if (shortName != null) {
				return shortName;
			}
			shortName = name.substring(name.lastIndexOf('.') + 1);
			int index = shortName.indexOf('/');
			if (index != -1) {
				shortName = shortName.substring(0, index) + shortName.substring(shortName.indexOf('#'));
			}
			return shortName;
		}
		
		protected void addChild(SampleMethod method) {
			children.add(method);
			method.parent = this;
			method.depth = depth + 1;
		}
		
		protected void increment() {
			count++;
		}
		
		/**
		 * @return The number of times this method appeared in samples - effectively,
		 * the number of milliseconds this method was being run for in total.
		 */
		public long getCount() {
			return count;
		}
		
		protected SampleMethod clone(SampleSummary summary) {
			SampleMethod root = new SampleMethod(summary, name);
			root.count = count;
			root.depth = depth;
			for (SampleMethod method : children) {
				root.addChild(method.clone(summary));
			}
			return root;
		}
		
	}
	
}
