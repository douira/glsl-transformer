package io.github.douira.glsl_transformer.transform;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import com.github.bsideup.jabel.Desugar;

/**
 * The transformation is the vehicle through which transformation phases, which
 * do all the actual transforming work, are added to the phase collector. It
 * also holds any inter-phase state that may be necessary for a transformation.
 * 
 * Since a transformation is independent of a phase collector since it only
 * contains a list of phases and their indexes, it could be added to multiple
 * phase collectors. However, they can't execute a single transformation in
 * separate threads as that would mess up the transformation's state.
 * 
 * Phases can, however, be added multiple times or to multiple phases. Beware of
 * sharing state between transformations as that could cause issues.
 * 
 * A stateless (no inter-phase state) transformation can be created by simply
 * making an instance of this class and adding transformations to it. If state
 * between phases is needed, make a subclass and add any state as instance
 * fields. Then phases are created and added within the subclass' constructor.
 * There cannot be any state stored as local variables either in the scope that
 * created the {@code Transformation} instance or in a subclass' constructor as
 * it will not be reset if a transformation is run multiple times. In the same
 * vein, state should only be initialized in the {@link #resetState()} method.
 * 
 * TODO: unclear if sharing phases between transformation managers is
 * problematic since then the compiled paths/patterns in phases have a different
 * parser than the one being used for the transformation. Probably it doesn't
 * matter and the parser is just used to figure out how the rules of the
 * tree are.
 */
public class Transformation<T> extends CollectorChildImpl<T> {
  /**
   * The default group index. If no group index is specified, this group index is
   * used. All phases without an explicit group index are added in this group. If
   * all phases are added without a group index, they are effectively only ordered
   * by their ordering index.
   */
  public static final int DEFAULT_GROUP = 0;

  /**
   * The default index. The first added phase has this index, even if it is added
   * with {@link #addConcurrentPhase(TransformationPhase)}.
   */
  public static final int DEFAULT_INDEX = 1;

  /**
   * The record used to store added transformation phases with their ordering
   * index and group index. When this transformation is added to a phase collector
   * the list of these entries is used to add the contained phases to the
   * collector.
   */
  @Desugar
  public static record PhaseEntry<T> (TransformationPhase<T> phase, int index, int group) {
  };

  private final List<PhaseEntry<T>> phaseRegistry = new ArrayList<>();
  private int nextPhaseIndex = DEFAULT_INDEX;
  private final List<Transformation<T>> childTransformations = new LinkedList<>();

  /**
   * Creates a stateless transformation and adds a single phase to it. If you
   * want to add multiple phases to a transformation, create an instance and call
   * {@link #addPhase(TransformationPhase)} multiple times.
   * 
   * @param phase The only transformation phase to add to a new stateless
   *              transformation
   */
  public Transformation(TransformationPhase<T> phase) {
    addPhase(phase);
  }

  /**
   * Creates a stateless transformation with no transformation phases, which can
   * be added later.
   */
  public Transformation() {
  }

  /**
   * Adds a transformation phase to this transformation. There is purposefully no
   * method that adds a whole array of phases as that would make the code harder
   * to read.
   * 
   * @param phase The transformation phase to append
   */
  public void addPhase(TransformationPhase<T> phase) {
    addPhase(nextPhaseIndex++, phase);
  }

  /**
   * Adds a transformation phase to this transformation at a specific order but in
   * the default group.
   * 
   * @param index The ordering index at which the phase should be executed
   * @param phase The transformation phase to insert
   */
  public void addPhase(int index, TransformationPhase<T> phase) {
    addPhase(index, getDefaultGroup(), phase);
  }

  /**
   * Adds a transformation phase to this transformation in a given group and at a
   * given position within that group. The index determines in which order the
   * phases will be executed in relation to other phases within this
   * transformation and within the phase collector. The group is like another
   * index that can be used to further separate transformations from eachother.
   * 
   * Choose an ascending index for phases to be executed in order. To separate all
   * phases of this transformation from those of other transformations, a
   * different group index should be used.
   * 
   * @param index The index at which the phase should be executed
   * @param group The index of the group in which this phase is executed at the
   *              given ordering index
   * @param phase The transformation phase to insert. For better formatting this
   *              parameter is at the end.
   */
  public void addPhase(int index, int group, TransformationPhase<T> phase) {
    addPhase(new PhaseEntry<>(phase, index, group));
  }

  /**
   * Adds a transformation phase entry to this transformation. The entry contains
   * a phase and information about when it should be executed by the phase
   * collector in relation to other phases in this and other transformations.
   * 
   * If the contained phase is null, the entry is ignored. This can be useful when
   * a phase is expected but it be a no-op.
   * 
   * @param entry The phase entry to add to the registry
   */
  public void addPhase(PhaseEntry<T> entry) {
    if (entry.phase() != null) {
      phaseRegistry.add(entry);
    }
  }

  /**
   * Adds a transformation phase to this transformation at the previous phase
   * counter position without incrementing the current phase counter. This means
   * the phase will run at the same time as the previous phase in the case of walk
   * phases. If no phase has been added yet, this will add the phase normally but
   * still not increment the phase counter.
   * 
   * @param phase The phase to add at the same position as the previous one
   */
  public void addConcurrentPhase(TransformationPhase<T> phase) {
    addPhase(Math.max(DEFAULT_INDEX, nextPhaseIndex - 1), phase);
  }

  /**
   * Adds a whole collection of phases to this transformation. This is meant for
   * programmatic use and not for adding phases individually.
   * 
   * @param entries The collection of phases to add
   */
  public void addPhases(Collection<PhaseEntry<T>> entries) {
    phaseRegistry.addAll(entries);
  }

  /**
   * Returns the default group for this transformation that is used for adding
   * phases if not specified otherwise. This method is meant to be overwritten by
   * transformation subclasses that want to use a different group for all their
   * phases.
   * 
   * @return The default group index to use for adding phases
   */
  protected int getDefaultGroup() {
    return DEFAULT_GROUP;
  }

  /**
   * This method is called by the phase collector each time a tree is transformed
   * in order to reset or initialize the state of the transformation if it has
   * any.
   */
  protected void resetState() {
  };

  /**
   * Inits the state of this transformation and all child transformations.
   */
  void resetStateInternal() {
    resetState();
    for (var child : childTransformations) {
      child.resetState();
    }
  }

  @Override
  public void setCollector(PhaseCollector<T> collector) {
    super.setCollector(collector);
    for (var child : childTransformations) {
      child.setCollector(collector);
    }
  }

  /**
   * Adds all the stored phases to the given phase collector.
   * 
   * @param collector The phase collector to add the phases to
   */
  void addPhasesTo(PhaseCollector<T> collector) {
    for (var entry : phaseRegistry) {
      collector.addPhaseAt(entry);
    }
  }

  /**
   * Adds all phases from another transformation into this one. Note that this
   * doesn't execute them after this transformation's phases but interleaved
   * according to the phase entry's index and group numbers.
   * 
   * @param other The other transformation to merge into this one
   */
  public void merge(Transformation<T> other) {
    addPhases(other.phaseRegistry);
    childTransformations.add(other);
  }

  /**
   * Appends all phases from another transformation to this one. This will adjust
   * the phase entry's indexes to be offset by the current next index as it
   * would be generated by {@link #addPhase(TransformationPhase)}. The next phase
   * added after this will be executed after the last phase of the appended
   * transformation. Note that group numbers are not modified. It also assumes
   * that the phases of the appended transformation start with index 1.
   * 
   * @param other The other transformation from which to append all phases onto
   *              this one
   */
  public void append(Transformation<T> other) {
    var maxIndex = -Integer.MIN_VALUE;
    for (var entry : other.phaseRegistry) {
      var index = entry.index;
      index += nextPhaseIndex - 1;
      addPhase(index, entry.group(), entry.phase());
      maxIndex = Math.max(maxIndex, index);
    }
    nextPhaseIndex = maxIndex + 1;
    childTransformations.add(other);
  }
}
