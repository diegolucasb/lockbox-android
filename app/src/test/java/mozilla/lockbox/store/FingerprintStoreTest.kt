/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.store

import android.app.KeyguardManager
import android.hardware.fingerprint.FingerprintManager
import org.junit.Assert
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.Mockito.`when` as whenCalled

@Suppress("DEPRECATION")
class FingerprintStoreTest {
    @Mock
    val fingerprintManager = Mockito.mock(FingerprintManager::class.java)

    @Mock
    val keyguardManager = Mockito.mock(KeyguardManager::class.java)

    val subject = FingerprintStore()

    @Test
    fun `isDeviceSecure when the device is fingerprint secure and not PIN or password secure`() {
        whenCalled(fingerprintManager.isHardwareDetected).thenReturn(true)
        whenCalled(fingerprintManager.hasEnrolledFingerprints()).thenReturn(true)
        whenCalled(keyguardManager.isDeviceSecure).thenReturn(false)

        subject.apply(fingerprintManager, keyguardManager)

        Assert.assertEquals(true, subject.isDeviceSecure)
    }

    @Test
    fun `isDeviceSecure when there is fingerprint hardware but there are no enrolled fingers and the device is not PIN or password secured`() {
        whenCalled(fingerprintManager.isHardwareDetected).thenReturn(true)
        whenCalled(fingerprintManager.hasEnrolledFingerprints()).thenReturn(false)
        whenCalled(keyguardManager.isDeviceSecure).thenReturn(false)

        subject.apply(fingerprintManager, keyguardManager)

        Assert.assertEquals(false, subject.isDeviceSecure)
    }

    @Test
    fun `isDeviceSecure when there is fingerprint hardware but there are no enrolled fingers but the device is not PIN or password secured`() {
        whenCalled(fingerprintManager.isHardwareDetected).thenReturn(true)
        whenCalled(fingerprintManager.hasEnrolledFingerprints()).thenReturn(false)
        whenCalled(keyguardManager.isDeviceSecure).thenReturn(true)

        subject.apply(fingerprintManager, keyguardManager)

        Assert.assertEquals(true, subject.isDeviceSecure)
    }

    @Test
    fun `isDeviceSecure when there is no fingerprint hardware and the device is not PIN or password secured`() {
        whenCalled(fingerprintManager.isHardwareDetected).thenReturn(false)
        whenCalled(fingerprintManager.hasEnrolledFingerprints()).thenReturn(false)
        whenCalled(keyguardManager.isDeviceSecure).thenReturn(false)

        subject.apply(fingerprintManager, keyguardManager)

        Assert.assertEquals(false, subject.isDeviceSecure)
    }

    @Test
    fun `isDeviceSecure when there is no fingerprint hardware but the device is PIN or password secured`() {
        whenCalled(fingerprintManager.isHardwareDetected).thenReturn(false)
        whenCalled(fingerprintManager.hasEnrolledFingerprints()).thenReturn(false)
        whenCalled(keyguardManager.isDeviceSecure).thenReturn(true)

        subject.apply(fingerprintManager, keyguardManager)

        Assert.assertEquals(true, subject.isDeviceSecure)
    }
}