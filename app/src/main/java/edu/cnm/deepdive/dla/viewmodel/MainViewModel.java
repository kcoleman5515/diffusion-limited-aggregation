/*
 *  Copyright 2022 CNM Ingenuity, Inc.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package edu.cnm.deepdive.dla.viewmodel;

import android.app.Application;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import edu.cnm.deepdive.dla.service.LatticeRepository;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import java.util.BitSet;
import java.util.Random;
import org.apache.commons.rng.simple.JDKRandomBridge;
import org.apache.commons.rng.simple.RandomSource;
import org.jetbrains.annotations.NotNull;

public class MainViewModel extends AndroidViewModel implements DefaultLifecycleObserver {

  private final LatticeRepository repository;
  private final MutableLiveData<BitSet> lattice;
  private final MutableLiveData<Boolean> running;
  private final LiveData<Random> rng;
  private final MutableLiveData<Throwable> throwable;
  private final CompositeDisposable pending;

  public MainViewModel(@NonNull Application application) {
    super(application);
    repository = new LatticeRepository(application);
    lattice = new MutableLiveData<>();
    running = new MutableLiveData<>(false);
    rng = new MutableLiveData<>(new JDKRandomBridge(RandomSource.XO_RO_SHI_RO_128_PP, null));
    throwable = new MutableLiveData<>();
    pending = new CompositeDisposable();
  }

  public LiveData<BitSet> getLattice() {
    return lattice;
  }

  public LiveData<Integer> getSize() {
    return repository.getSize();
  }

  public LiveData<Boolean> getRunning() {
    return running;
  }

  public void setRunning(boolean running) {
    if (running) {
      accumulate();
    }
    this.running.setValue(running);
  }

  public LiveData<Random> getRng() {
    return rng;
  }

  public LiveData<Throwable> getThrowable() {
    return throwable;
  }

  public void accumulate() {
    throwable.postValue(null);
    repository
        .accumulate(1)
        .subscribe(
            (lattice) -> {
              this.lattice.postValue(lattice);
              accumulate();
            },
            (throwable) -> {
              running.postValue(false);
              postThrowable(throwable);
            },
            pending
        );
  }

  public void clear() {
    throwable.setValue(null);
    repository
        .clear()
        .subscribe(
            lattice::postValue,
            this::postThrowable,
            pending
        );
  }

  public void set(int x, int y) {
    throwable.setValue(null);
    repository
        .set(x, y)
        .subscribe(
            lattice::postValue,
            this::postThrowable,
            pending
        );
  }

  @Override
  public void onStop(@NonNull @NotNull LifecycleOwner owner) {
    pending.clear();
    DefaultLifecycleObserver.super.onStop(owner);
  }

  private void postThrowable(Throwable throwable) {
    Log.e(getClass().getSimpleName(), throwable.getMessage(), throwable);
    this.throwable.postValue(throwable);
  }

}
