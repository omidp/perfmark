package io.perfmark.impl;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Nullable;

public final class Storage {
  private static final long INIT_NANO_TIME = System.nanoTime();

  // The order of initialization here matters.  If a logger invokes PerfMark, it will be re-entrant
  // and need to use these static variables.
  static final ConcurrentMap<MarkHolderRef, Boolean> allMarkHolders =
      new ConcurrentHashMap<MarkHolderRef, Boolean>();
  private static final ThreadLocal<MarkHolder> localMarkHolder = new MarkHolderThreadLocal();
  static final MarkHolderProvider markHolderProvider;
  private static final Logger logger;

  static {
    List<MarkHolderProvider> providers = new ArrayList<MarkHolderProvider>();
    List<Throwable> fines = new ArrayList<Throwable>();
    List<Throwable> warnings = new ArrayList<Throwable>();
    Class<?> clz = null;
    try {
      clz =
          Class.forName(
              "io.perfmark.java9.SecretVarHandleMarkHolderProvider$VarHandleMarkHolderProvider");
    } catch (ClassNotFoundException e) {
      fines.add(e);
    } catch (Throwable t) {
      warnings.add(t);
    }
    if (clz != null) {
      try {
        providers.add(clz.asSubclass(MarkHolderProvider.class).getConstructor().newInstance());
      } catch (Throwable t) {
        warnings.add(t);
      }
      clz = null;
    }
    try {
      clz =
          Class.forName(
              "io.perfmark.java6.SecretSynchronizedMarkHolderProvider$SynchronizedMarkHolderProvider");
    } catch (ClassNotFoundException e) {
      fines.add(e);
    } catch (Throwable t) {
      warnings.add(t);
    }
    if (clz != null) {
      try {
        providers.add(clz.asSubclass(MarkHolderProvider.class).getConstructor().newInstance());
      } catch (Throwable t) {
        warnings.add(t);
      }
      clz = null;
    }

    if (!providers.isEmpty()) {
      markHolderProvider = providers.get(0);
    } else {
      markHolderProvider = new NoopMarkHolderProvider();
    }

    logger = Logger.getLogger(Storage.class.getName());

    for (Throwable error : warnings) {
      logger.log(Level.WARNING, "Error loading MarkHolderProvider", error);
    }
    for (Throwable error : fines) {
      logger.log(Level.FINE, "Error loading MarkHolderProvider", error);
    }
  }

  public static long getInitNanoTime() {
    return INIT_NANO_TIME;
  }

  /** Returns a list of {@link MarkList}s across all reachable threads. */
  public static List<MarkList> read() {
    List<MarkHolderRef> markHolderRefs = new ArrayList<MarkHolderRef>();
    MarkHolderRef.cleanQueue(markHolderRefs, allMarkHolders);
    // Capture a snapshot of the index with as little skew as possible.  Don't pre-size the lists
    // since it would mean scanning allMarkHolders twice.  Instead, try to get a strong ref to each
    // of the MarkHolders before they could get GC'd.
    markHolderRefs.addAll(allMarkHolders.keySet());
    List<MarkList> markLists = new ArrayList<MarkList>(markHolderRefs.size());
    readInto(markLists, markHolderRefs);
    return Collections.unmodifiableList(markLists);
  }

  private static void readInto(
      List<? super MarkList> markLists, List<? extends MarkHolderRef> markHolderRefs) {
    for (MarkHolderRef ref : markHolderRefs) {
      final String threadName;
      @Nullable Thread writer = ref.get();
      if (writer != null) {
        ref.lastThreadName.set(threadName = writer.getName());
      } else {
        threadName = ref.lastThreadName.get();
      }
      boolean concurrentWrites = !(Thread.currentThread() == writer || writer == null);
      markLists.add(
          MarkList.newBuilder()
              .setMarks(ref.holder.read(concurrentWrites))
              .setThreadName(threadName)
              .setThreadId(ref.threadId)
              .setMarkListId(ref.markHolderId)
              .build());
    }
  }

  static void startAnyways(long gen, String taskName, @Nullable String tagName, long tagId) {
    localMarkHolder.get().start(gen, taskName, tagName, tagId, System.nanoTime());
  }

  static void startAnyways(
      long gen, String taskName, Marker marker, @Nullable String tagName, long tagId) {
    localMarkHolder.get().start(gen, taskName, marker, tagName, tagId, System.nanoTime());
  }

  static void startAnyways(long gen, String taskName) {
    localMarkHolder.get().start(gen, taskName, System.nanoTime());
  }

  static void startAnyways(long gen, String taskName, Marker marker) {
    localMarkHolder.get().start(gen, taskName, marker, System.nanoTime());
  }

  static void stopAnyways(long gen, String taskName, @Nullable String tagName, long tagId) {
    long nanoTime = System.nanoTime();
    localMarkHolder.get().stop(gen, taskName, tagName, tagId, nanoTime);
  }

  static void stopAnyways(
      long gen, String taskName, Marker marker, @Nullable String tagName, long tagId) {
    long nanoTime = System.nanoTime();
    localMarkHolder.get().stop(gen, taskName, marker, tagName, tagId, nanoTime);
  }

  static void stopAnyways(long gen, String taskName) {
    long nanoTime = System.nanoTime();
    localMarkHolder.get().stop(gen, taskName, nanoTime);
  }

  static void stopAnyways(long gen, String taskName, Marker marker) {
    long nanoTime = System.nanoTime();
    localMarkHolder.get().stop(gen, taskName, marker, nanoTime);
  }

  static void eventAnyways(long gen, String eventName, @Nullable String tagName, long tagId) {
    long nanoTime = System.nanoTime();
    localMarkHolder.get().event(gen, eventName, tagName, tagId, nanoTime, 0);
  }

  static void eventAnyways(
      long gen, String taskName, Marker marker, @Nullable String tagName, long tagId) {
    long nanoTime = System.nanoTime();
    localMarkHolder.get().event(gen, taskName, marker, tagName, tagId, nanoTime, 0);
  }

  static void eventAnyways(long gen, String eventName) {
    long nanoTime = System.nanoTime();
    localMarkHolder.get().event(gen, eventName, nanoTime, 0);
  }

  static void eventAnyways(long gen, String taskName, Marker marker) {
    long nanoTime = System.nanoTime();
    localMarkHolder.get().event(gen, taskName, marker, nanoTime, 0);
  }

  static void linkAnyways(long gen, long linkId) {
    localMarkHolder.get().link(gen, linkId);
  }

  static void linkAnyways(long gen, long linkId, Marker marker) {
    localMarkHolder.get().link(gen, linkId, marker);
  }

  public static void resetForTest() {
    localMarkHolder.remove();
  }

  @Nullable
  public static MarkList readForTest() {
    MarkHolder mh = localMarkHolder.get();
    for (MarkHolderRef ref : allMarkHolders.keySet()) {
      if (ref.holder == mh) {
        List<MarkList> markHolders = new ArrayList<MarkList>(1);
        readInto(markHolders, Collections.singletonList(ref));
        return markHolders.get(0);
      }
    }
    return null;
  }

  private static final class MarkHolderThreadLocal extends ThreadLocal<MarkHolder> {

    MarkHolderThreadLocal() {}

    @Override
    protected MarkHolder initialValue() {
      MarkHolderRef.cleanQueue(null, allMarkHolders);
      MarkHolder holder = markHolderProvider.create();
      MarkHolderRef ref = new MarkHolderRef(Thread.currentThread(), holder);
      allMarkHolders.put(ref, Boolean.TRUE);
      return holder;
    }
  }

  private static final class MarkHolderRef extends WeakReference<Thread> {
    private static final ReferenceQueue<Thread> markHolderRefQueue = new ReferenceQueue<Thread>();
    private static final AtomicLong markHolderIdAllocator = new AtomicLong();

    final MarkHolder holder;
    final long markHolderId = markHolderIdAllocator.incrementAndGet();
    final long threadId;
    final AtomicReference<String> lastThreadName;

    MarkHolderRef(Thread thread, MarkHolder holder) {
      super(thread, markHolderRefQueue);
      this.holder = holder;
      this.threadId = thread.getId();
      this.lastThreadName = new AtomicReference<String>(thread.getName());
    }

    static void cleanQueue(
        @Nullable Collection<? super MarkHolderRef> deadRefs, Map<?, ?> allSpans) {
      MarkHolderRef ref;
      while ((ref = (MarkHolderRef) markHolderRefQueue.poll()) != null) {
        ref.clear();
        allSpans.remove(ref);
        if (deadRefs != null) {
          deadRefs.add(ref);
        }
      }
    }
  }

  private Storage() {
    throw new AssertionError("nope");
  }
}
