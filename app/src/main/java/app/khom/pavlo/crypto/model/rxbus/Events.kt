package app.khom.pavlo.crypto.model.rxbus



class CoinsLoadingEvent(val isLoading: Boolean)

class OnDeleteCoinsMenuItemClickedEvent

class MainCoinsListUpdatedEvent

class SearchHashTagUpdated(val hashTag: String)

class CoinsSortMethodUpdated(val sort: String?)

class LanguageChanged(val language: String)
