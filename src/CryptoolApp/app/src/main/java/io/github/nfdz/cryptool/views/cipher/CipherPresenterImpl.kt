package io.github.nfdz.cryptool.views.cipher

import java.util.concurrent.atomic.AtomicInteger


class CipherPresenterImpl(
    private var view: CipherContract.View?,
    private var interactor: CipherContract.Interactor?
) : CipherContract.Presenter {

    companion object {
        const val PROCESSING_TEXT = "⌛"
        const val ERROR_TEXT = "✖"
    }

    private var isPassphraseLocked: Boolean = false
    private var isPassphraseVisible: Boolean = false
    private var processCounter = AtomicInteger()

    override fun onCreate() {
        interactor?.let {
            view?.setCipherMode(it.getLastMode())
            view?.setPassphrase(it.getLastPassphrase())
            view?.setOriginText(it.getLastOriginText())
            isPassphraseLocked = it.wasLastPassphraseLocked()
            view?.setPassphraseMode(isPassphraseVisible, !isPassphraseLocked)
        }
        processOriginText()
    }

    override fun onDestroy() {
        view = null
        interactor = null
    }

    override fun onPassphraseTextChanged() {
        clearPassphraseLock()
        processOriginText()
    }

    private fun clearPassphraseLock() {
        if (isPassphraseLocked) {
            val passphrase = view?.getPassphrase() ?: ""
            if (passphrase == "") {
                isPassphraseLocked = false
                view?.setPassphraseMode(isPassphraseVisible, !isPassphraseLocked)
            }
        }

    }

    override fun onOriginTextChanged() {
        processOriginText()
    }

    override fun onToggleModeClick() {
        view?.setCipherMode(
            when (view?.getCipherMode()) {
                CipherContract.ModeFlag.ENCRYIPT_MODE -> CipherContract.ModeFlag.DECRYIPT_MODE
                CipherContract.ModeFlag.DECRYIPT_MODE -> CipherContract.ModeFlag.ENCRYIPT_MODE
                null -> CipherContract.DEFAULT_MODE
            }
        )
        var processedText = view?.getProcessedText() ?: ""
        if (processedText == PROCESSING_TEXT || processedText == ERROR_TEXT) {
            processedText = ""
        }
        view?.setOriginText(processedText)
        processOriginText()
    }

    private fun processOriginText() {
        val expectedProcessCounter = processCounter.incrementAndGet()
        val passphrase = view?.getPassphrase() ?: ""
        val originText = view?.getOriginText() ?: ""
        if (passphrase == "" || originText == "") {
            view?.setProcessedText("")
            saveState()
        } else {
            view?.setProcessedText(PROCESSING_TEXT)
            val success: (String) -> (Unit) = { processedText ->
                if (processCounter.get() == expectedProcessCounter) {
                    view?.setProcessedText(processedText)
                    saveState()
                }
            }
            val error: () -> (Unit) = {
                if (processCounter.get() == expectedProcessCounter) {
                    view?.setProcessedText(ERROR_TEXT)
                    saveState()
                }
            }
            when (view?.getCipherMode()) {
                CipherContract.ModeFlag.ENCRYIPT_MODE -> {
                    interactor?.encrypt(passphrase, originText, success, error)
                }
                CipherContract.ModeFlag.DECRYIPT_MODE -> {
                    interactor?.decrypt(passphrase, originText, success, error)
                }
            }
        }

    }

    override fun onViewPassphraseClick() {
        if (!isPassphraseLocked) {
            isPassphraseVisible = !isPassphraseVisible
            view?.setPassphraseMode(isPassphraseVisible, !isPassphraseLocked)
        }
    }

    override fun onLockPassphraseClick() {
        val passphrase = view?.getPassphrase() ?: ""
        if (passphrase != "") {
            isPassphraseLocked = true
            isPassphraseVisible = false
            view?.setPassphraseMode(isPassphraseVisible, !isPassphraseLocked)
            saveState()
        }
    }

    private fun saveState() {
        interactor?.saveState(
            view?.getCipherMode(),
            view?.getPassphrase(),
            isPassphraseLocked,
            view?.getOriginText()
        )
    }

}