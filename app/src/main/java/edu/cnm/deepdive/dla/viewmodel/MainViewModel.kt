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
package edu.cnm.deepdive.dla.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.*
import edu.cnm.deepdive.dla.service.LatticeRepository
import io.reactivex.rxjava3.disposables.CompositeDisposable
import org.apache.commons.rng.simple.JDKRandomBridge
import org.apache.commons.rng.simple.RandomSource
import java.util.*

class MainViewModel(application: Application) : AndroidViewModel(application),
    DefaultLifecycleObserver {

    private val repository = LatticeRepository(application)
    private val pending = CompositeDisposable()

    val rng: LiveData<Random> =
        MutableLiveData(JDKRandomBridge(RandomSource.XO_RO_SHI_RO_128_PP, null))

    private val _lattice: MutableLiveData<BitSet> = MutableLiveData()
    val lattice: LiveData<BitSet>
        get() = _lattice

    private val _running: MutableLiveData<Boolean> = MutableLiveData(false)
    val running: LiveData<Boolean>
        get() = _running

    private val _throwable: MutableLiveData<Throwable?> = MutableLiveData()
    val throwable: LiveData<Throwable?>
        get() = _throwable



    val size: LiveData<Int>
        get() = repository.size

    fun resume() {
        accumulate()
        _running.value = true
    }

    fun pause() {
        _running.value = false
    }


    fun accumulate() {
        _throwable.postValue(null)
        repository
            .accumulate(1)
            .subscribe(
                { lattice: BitSet ->
                    _lattice.postValue(lattice)
                    running.value?.run {
                        accumulate()
                    }
                },
                {
                    _running.postValue(false)
                    postThrowable(it)
                },
                pending
            )
    }

    fun clear() {
        _throwable.value = null
        repository
            .clear()
            .subscribe(
                { _lattice.postValue(it) },
                { postThrowable(it) },
                pending
            )
    }

    operator fun set(x: Int, y: Int) {
        _throwable.value = null
        repository
            .set(x, y)
            .subscribe(
                { _lattice.postValue(it) },
                { postThrowable(it) },
                pending
            )
    }

    override fun onStop(owner: LifecycleOwner) {
        pending.clear()
        super.onStop(owner)
    }

    private fun postThrowable(throwable: Throwable) {
        Log.e(javaClass.simpleName, throwable.message, throwable)
        _throwable.postValue(throwable)
    }
}