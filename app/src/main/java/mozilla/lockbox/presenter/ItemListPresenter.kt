/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.presenter

import android.support.annotation.IdRes
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.functions.Consumer
import io.reactivex.rxkotlin.addTo
import mozilla.lockbox.R
import mozilla.lockbox.action.DataStoreAction
import mozilla.lockbox.action.RouteAction
import mozilla.lockbox.extensions.AlertState
import mozilla.lockbox.extensions.mapToItemViewModelList
import mozilla.lockbox.flux.Dispatcher
import mozilla.lockbox.flux.Presenter
import mozilla.lockbox.log
import mozilla.lockbox.model.ItemViewModel
import mozilla.lockbox.store.DataStore
import mozilla.lockbox.store.FingerprintStore

interface ItemListView {
    val itemSelection: Observable<ItemViewModel>
    val filterClicks: Observable<Unit>
    val menuItemSelections: Observable<Int>
    fun updateItems(itemList: List<ItemViewModel>)
    fun displayPINDisclaimer(dialogObserver: Consumer<AlertState>)
}

class ItemListPresenter(
    private val view: ItemListView,
    private val dispatcher: Dispatcher = Dispatcher.shared,
    private val dataStore: DataStore = DataStore.shared,
    private val fingerprintStore: FingerprintStore = FingerprintStore.shared
) : Presenter() {
    private val disclaimerDialogConsumer: Consumer<AlertState>
        get() = Consumer {
            when (it) {
                AlertState.BUTTON_POSITIVE -> {
                    dispatcher.dispatch(RouteAction.SystemSetting(android.provider.Settings.ACTION_SECURITY_SETTINGS))
                }
                else -> {
                    // do nothing
                }
            }
        }

    override fun onViewReady() {
        dataStore.list
                .filter { it.isNotEmpty() }
                .mapToItemViewModelList()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(view::updateItems)
                .addTo(compositeDisposable)

        view.itemSelection
                .subscribe { it ->
                    dispatcher.dispatch(RouteAction.ItemDetail(it.guid))
                }
                .addTo(compositeDisposable)

        view.filterClicks
                .subscribe {
                    dispatcher.dispatch(RouteAction.Filter)
                }
                .addTo(compositeDisposable)

        view.menuItemSelections
            .subscribe(this::onMenuItem)
            .addTo(compositeDisposable)

        // TODO: remove this when we have proper locking / unlocking
        dispatcher.dispatch(DataStoreAction.Unlock)
    }

    private fun onMenuItem(@IdRes item: Int) {
        if (item == R.id.fragment_locked && !fingerprintStore.isDeviceSecure) {
            view.displayPINDisclaimer(disclaimerDialogConsumer)
            return
        }

        val action = when (item) {
            R.id.fragment_locked -> RouteAction.LockScreen
            R.id.fragment_setting -> RouteAction.SettingList
            else -> return log.error("Cannot route from item list menu")
        }

        dispatcher.dispatch(action)
    }
}