package io.perfmark;

import com.google.errorprone.annotations.CompileTimeConstant;
import com.google.errorprone.annotations.DoNotCall;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * PerfMark can be automatically enabled by setting the System property {@code
 * io.perfmark.PerfMark.startEnabled} to true.
 */
public final class PerfMark {
  private static final Impl impl;

  static {
    Impl instance = null;
    Level level = Level.WARNING;
    Throwable err = null;
    Class<?> clz = null;
    try {
      clz = Class.forName("io.perfmark.impl.SecretPerfMarkImpl$PerfMarkImpl");
    } catch (ClassNotFoundException e) {
      level = Level.FINE;
      err = e;
    } catch (Throwable t) {
      err = t;
    }
    if (clz != null) {
      try {
        instance = clz.asSubclass(Impl.class).getConstructor(Tag.class).newInstance(Impl.NO_TAG);
      } catch (Throwable t) {
        err = t;
      }
    }
    if (instance != null) {
      impl = instance;
    } else {
      impl = new Impl(Impl.NO_TAG);
    }
    if (err != null) {
      Logger.getLogger(PerfMark.class.getName()).log(level, "Error during PerfMark.<clinit>", err);
    }
  }

  /**
   * Turns on or off PerfMark recording. Don't call this method too frequently; while neither on nor
   * off have very high overhead, transitioning between the two may be slow.
   *
   * @param value {@code true} to enable PerfMark recording, or {@code false} to disable it.
   */
  public static void setEnabled(boolean value) {
    impl.setEnabled(value);
  }

  /**
   * Marks the beginning of a task. If PerfMark is disabled, this method is a no-op. The name of the
   * task should be a compile-time constant, usually a string literal. Tasks with the same name can
   * be grouped together for analysis later, so avoid using too many unique task names.
   *
   * <p>The tag is a run-time identifier for the task. It represents the dynamic part of the task,
   * while the task name is the constant part of the task. While not always enforced, tags should
   * not be {@code null}.
   *
   * @param taskName the name of the task.
   * @param tag a user provided tag for the task.
   */
  public static void startTask(@CompileTimeConstant String taskName, Tag tag) {
    impl.startTask(taskName, tag);
  }

  /**
   * Marks the beginning of a task. If PerfMark is disabled, this method is a no-op. The name of the
   * task should be a compile-time constant, usually a string literal. Tasks with the same name can
   * be grouped together for analysis later, so avoid using too many unique task names.
   *
   * @param taskName the name of the task.
   */
  public static void startTask(@CompileTimeConstant String taskName) {
    impl.startTask(taskName);
  }

  /**
   * Marks an event. Events are logically both a task start and a task end. Events have no duration
   * associated. Events still represent the instant something occurs. If PerfMark is disabled, this
   * method is a no-op.
   *
   * <p>The tag is a run-time identifier for the event. It represents the dynamic part of the event,
   * while the event name is the constant part of the event. While not always enforced, tags should
   * not be {@code null}.
   *
   * @param eventName the name of the event.
   * @param tag a user provided tag for the event.
   */
  public static void event(@CompileTimeConstant String eventName, Tag tag) {
    impl.event(eventName, tag);
  }

  /**
   * Marks an event. Events are logically both a task start and a task end. Events have no duration
   * associated. Events still represent the instant something occurs. If PerfMark is disabled, this
   * method is a no-op.
   *
   * @param eventName the name of the event.
   */
  public static void event(@CompileTimeConstant String eventName) {
    impl.event(eventName);
  }

  /**
   * Marks the end of a task. If PerfMark is disabled, this method is a no-op. The task name and tag
   * should match the ones provided to the corresponding {@link #startTask(String, Tag)}. If the
   * task name or tag do not match, the implementation may not be able to associate the starting and
   * stopping of a single task. The name of the task should be a compile-time constant, usually a
   * string literal.
   *
   * <p>It is important that {@link #stopTask} always be called after starting a task, even in case
   * of exceptions. Failing to do so may result in corrupted results.
   *
   * @param taskName the name of the task being ended.
   * @param tag the tag of the task being ended.
   */
  public static void stopTask(@CompileTimeConstant String taskName, Tag tag) {
    impl.stopTask(taskName, tag);
  }

  /**
   * Marks the end of a task. If PerfMark is disabled, this method is a no-op. The task name should
   * match the ones provided to the corresponding {@link #startTask(String)}. If the task name or
   * tag do not match, the implementation may not be able to associate the starting and stopping of
   * a single task. The name of the task should be a compile-time constant, usually a string
   * literal.
   *
   * <p>It is important that {@link #stopTask} always be called after starting a task, even in case
   * of exceptions. Failing to do so may result in corrupted results.
   *
   * @param taskName the name of the task being ended.
   */
  public static void stopTask(@CompileTimeConstant String taskName) {
    impl.stopTask(taskName);
  }

  /**
   * Creates a tag with no name or numeric identifier. The returned instance is different based on
   * if PerfMark is enabled or not.
   *
   * <p>This method is seldomly useful; users should generally prefer to use the overloads of
   * methods that don't need a tag. An empty tag may be useful though when the tag of a group of
   * tasks may change over time.
   *
   * @return a Tag that has no name or id.
   */
  public static Tag createTag() {
    return Impl.NO_TAG;
  }

  /**
   * Creates a tag with no name. The returned instance is different based on if PerfMark is enabled
   * or not. The provided id does not have to be globally unique, but is instead meant to give
   * context to a task.
   *
   * @param id a user provided identifier for this Tag.
   * @return a Tag that has no name.
   */
  public static Tag createTag(long id) {
    return impl.createTag(Impl.NO_TAG_NAME, id);
  }

  /**
   * Creates a tag with no numeric identifier. The returned instance is different based on if
   * PerfMark is enabled or not. The provided name does not have to be globally unique, but is
   * instead meant to give context to a task.
   *
   * @param name a user provided name for this Tag.
   * @return a Tag that has no numeric identifier.
   */
  public static Tag createTag(String name) {
    return impl.createTag(name, Impl.NO_TAG_ID);
  }

  /**
   * Creates a tag with both a name and a numeric identifier. The returned instance is different
   * based on if PerfMark is enabled or not. Neither the provided name nor id has to be globally
   * unique, but are instead meant to give context to a task.
   *
   * @param id a user provided identifier for this Tag.
   * @param name a user provided name for this Tag.
   * @return a Tag that has both a name and id.
   */
  public static Tag createTag(String name, long id) {
    return impl.createTag(name, id);
  }

  /** DO NOT CALL, no longer implemented. Use {@link #linkOut} instead. */
  @Deprecated
  @DoNotCall
  public static Link link() {
    return Impl.NO_LINK;
  }

  /**
   * A link connects between two tasks that start asynchronously. When {@link #linkOut()} is called,
   * an association between the most recently started task and a yet-to-be named task on another
   * thread, is created. Links are a one-to-many relationship. A single started task can have
   * multiple associated tasks on other threads.
   *
   * @since 0.17.0
   * @return A Link to be used in other tasks.
   */
  public static Link linkOut() {
    return impl.linkOut();
  }

  /**
   * Associate this link with the most recently started task. There may be at most one inbound
   * linkage per task: the first call to {@link #linkIn} decides which outbound task is the origin.
   *
   * @since 0.17.0
   */
  public static void linkIn(Link link) {
    impl.linkIn(link);
  }

  private PerfMark() {}
}
