package tanzent.cassette.misc.exception

class MultipChoiceException(cause: Throwable?, val multiChoice: String) : Exception(cause) {

}