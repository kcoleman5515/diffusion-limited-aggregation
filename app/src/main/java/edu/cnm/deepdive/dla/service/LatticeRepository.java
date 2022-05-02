package edu.cnm.deepdive.dla.service;

import android.content.Context;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import edu.cnm.deepdive.dla.R;
import edu.cnm.deepdive.dla.model.Lattice;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.schedulers.Schedulers;
import java.util.BitSet;
import java.util.Random;
import org.apache.commons.rng.simple.JDKRandomBridge;
import org.apache.commons.rng.simple.RandomSource;

public class LatticeRepository {

  private final String noAggregateMessage;
  private final String boundaryReachedMessage;
  private final Lattice lattice;
  private final MutableLiveData<Integer> size;

  public LatticeRepository(Context context) {
    noAggregateMessage = context.getString(R.string.no_aggregate_message);
    boundaryReachedMessage = context.getString(R.string.boundary_reached_message);
    Random rng = new JDKRandomBridge(RandomSource.XO_RO_SHI_RO_128_PP, null);
    lattice = new Lattice.Builder()
        .rng(rng)
        .build();
    size = new MutableLiveData<>(lattice.getSize());
  }

  public LiveData<Integer> getSize() {
    return size;
  }

  public Single<BitSet> clear() {
    return Single
        .fromCallable(() -> {
          lattice.clear();
          return lattice.get();
        })
        .subscribeOn(Schedulers.computation());
  }

  public Single<BitSet> set(int x, int y) {
    return Single
        .fromCallable(() -> {
          lattice.set(x, y);
          return lattice.get();
        })
        .subscribeOn(Schedulers.computation());
  }

  public Single<BitSet> accumulate(int deltaMass) {
    return Single
        .fromCallable(() -> {
          if (lattice.getMass() == 0) {
            throw new NoAggregateException(noAggregateMessage);
          }
          for (int i = 0; i < deltaMass; i++) {
            if (lattice.isBoundaryReached()) {
              throw new BoundaryReachedException(boundaryReachedMessage);
            }
            lattice.accumulate();
          }
          return lattice.get();
        })
        .subscribeOn(Schedulers.computation());
  }

  public static class NoAggregateException extends IllegalStateException {

    public NoAggregateException() {
    }

    public NoAggregateException(String message) {
      super(message);
    }

    public NoAggregateException(String message, Throwable cause) {
      super(message, cause);
    }

    public NoAggregateException(Throwable cause) {
      super(cause);
    }
  }

  public static class BoundaryReachedException extends IllegalStateException {

    public BoundaryReachedException() {
    }

    public BoundaryReachedException(String message) {
      super(message);
    }

    public BoundaryReachedException(String message, Throwable cause) {
      super(message, cause);
    }

    public BoundaryReachedException(Throwable cause) {
      super(cause);
    }

  }
}
