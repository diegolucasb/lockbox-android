/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.presenter

import io.reactivex.Observable
import io.reactivex.observers.TestObserver
import io.reactivex.subjects.PublishSubject
import mozilla.lockbox.R
import mozilla.lockbox.action.RouteAction
import mozilla.lockbox.action.SettingIntent
import mozilla.lockbox.extensions.AlertState
import mozilla.lockbox.extensions.assertLastValue
import mozilla.lockbox.flux.Action
import mozilla.lockbox.flux.Dispatcher
import mozilla.lockbox.model.ItemViewModel
import mozilla.lockbox.store.DataStore
import mozilla.lockbox.store.FingerprintStore
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito
import org.mozilla.sync15.logins.ServerPassword
import org.robolectric.RobolectricTestRunner
import org.mockito.Mockito.`when` as whenCalled

@RunWith(RobolectricTestRunner::class)
open class ItemListPresenterTest {
    class FakeView : ItemListView {
        val itemSelectedStub = PublishSubject.create<ItemViewModel>()

        val filterClickStub = PublishSubject.create<Unit>()
        val menuItemSelectionStub = PublishSubject.create<Int>()
        var updateItemsArgument: List<ItemViewModel>? = null

        val disclaimerActionStub = PublishSubject.create<AlertState>()

        override val itemSelection: Observable<ItemViewModel>
            get() = itemSelectedStub

        override val filterClicks: Observable<Unit>
            get() = filterClickStub

        override val menuItemSelections: Observable<Int>
            get() = menuItemSelectionStub

        override fun updateItems(itemList: List<ItemViewModel>) {
            updateItemsArgument = itemList
        }
    }

    class FakeDataStore : DataStore() {
        val listStub = PublishSubject.create<List<ServerPassword>>()

        override val list: Observable<List<ServerPassword>>
            get() = listStub
    }

    @Mock
    val fingerprintStore = Mockito.mock(FingerprintStore::class.java)

    val view = FakeView()
    val dataStore = FakeDataStore()
    lateinit var subject: ItemListPresenter

    val dispatcherObserver = TestObserver.create<Action>()

    @Before
    fun setUp() {
        Dispatcher.shared.register.subscribe(dispatcherObserver)

        subject = ItemListPresenter(view, dataStore = dataStore, fingerprintStore = fingerprintStore)
        subject.onViewReady()
    }

    @Test
    fun receivingItemSelected() {
        val id = "the_guid"
        view.itemSelectedStub.onNext(ItemViewModel("title", "subtitle", id))

        dispatcherObserver.assertLastValue(RouteAction.ItemDetail(id))
    }

    @Test
    fun receivingPasswordList_somePasswords() {
        val username = "dogs@dogs.com"
        val password1 = ServerPassword(
                "fdsfda",
                "https://www.mozilla.org",
                username,
                "woof",
                timesUsed = 0,
                timeCreated = 0L,
                timeLastUsed = 0L,
                timePasswordChanged = 0L)
        val password2 = ServerPassword("ghfdhg",
                "https://www.cats.org",
                username,
                "meow",
                timesUsed = 0,
                timeCreated = 0L,
                timeLastUsed = 0L,
                timePasswordChanged = 0L)
        val password3 = ServerPassword("ioupiouiuy",
                "www.dogs.org",
                password = "baaaaa",
                username = null,
                timesUsed = 0,
                timeCreated = 0L,
                timeLastUsed = 0L,
                timePasswordChanged = 0L)
        val list = listOf(password1, password2, password3)

        dataStore.listStub.onNext(list)

        val expectedList = listOf<ItemViewModel>(
                ItemViewModel("mozilla.org",
                        username,
                        password1.id),
                ItemViewModel("cats.org",
                        username,
                        password2.id),
                ItemViewModel("dogs.org",
                        "",
                        password3.id)
        )

        Assert.assertEquals(expectedList, view.updateItemsArgument)
    }

    @Test
    fun receivingPasswordList_empty() {
        dataStore.listStub.onNext(emptyList())

        Assert.assertNull(view.updateItemsArgument)
    }

    @Test
    fun `menuItem clicks cause RouteActions`() {
        view.menuItemSelectionStub.onNext(R.id.fragment_setting)
        dispatcherObserver.assertLastValue(RouteAction.SettingList)
    }

    @Test
    fun `tapping on the lock menu item when the user has no device security routes to security disclaimer dialog`() {
        whenCalled(fingerprintStore.isDeviceSecure).thenReturn(false)
        setUp()
        view.menuItemSelectionStub.onNext(R.id.fragment_locked)

        view.disclaimerActionStub.onNext(AlertState.BUTTON_POSITIVE)
        val routeAction = dispatcherObserver.values().last() as RouteAction.DialogAction

        Assert.assertTrue(routeAction is RouteAction.DialogAction.SecurityDisclaimerDialog)

        Assert.assertEquals(RouteAction.SystemSetting(SettingIntent.Security), routeAction.positiveButtonAction)
    }

    @Test
    fun `tapping on the lock menu item when the user has device security routes to lock screen`() {
        whenCalled(fingerprintStore.isDeviceSecure).thenReturn(true)
        view.menuItemSelectionStub.onNext(R.id.fragment_locked)
        dispatcherObserver.assertLastValue(RouteAction.LockScreen)
    }
}
