# My notes from the book

## Chapter 1: Introduction
### Threads
```
private fun fetchTextWithThread() {
    Thread {
        val text = getTextFromNetwork()
    }.start()
}
```
Can do any operation except updating UI, which will result in `NetworkOnMainThreadException`

To update UI,
```
private fun fetchTextWithThread() {
    Thread {
        val text = getTextFromNetwork()
        runOnUiThread {
            displayText(text)
        }
    }.start()
}
```

If you are not starting the thread from an activity, you can use handlers instead of `runOnUiThread` to update the UI
Background Thread -> Handler -> Main Thread
*A handler (android.os.Handler) allows communication between threads, such as background thread to main thread*
Pass a looper into the Handler constructor to specify the thread where the task will be run
*A looper is an object that runs the messages in the thread's queue*
```
private fun fetchTextWithThreadAndHandler() {
    Thread {
        val text = getTextFromNetwork()
        Handler(Looper.getMainLooper()).post {
            displayText(text)
        }
    }.start()
}
```
`Handler(Looper.getMainLooper())` creates a handler tied to the main thread and posts the `displayText()` runnable function on the main thread
`Handler.post(Runnable)` enqueues the runnable function to be executed on the specified thread.
Other variants include postAtTime and postDelayed

A thread's handler allows you to send a message to the thread's message queue.
The handler's looper will execute the messages in the queue.

Use `setData(Bundle)` to pass a single bundle of data in the Message object
Then, create a subclass of Handler and override the `handleMessage(Message)` function.
There, you can then get the data from the message and process it in the handler's thread.
Can use `sendMessage(Messsage` to send a message
```
private val key = "key"
private val messageHandler = object :
    Handler(Looper.getMainLooper()) {
        override fun handleMessage(message: Message) {
            val bundle = message.data
            val text = bundle.getString(key, "")
            displayText()
        }
    }
}
private fun fetchTextWithHandlerMessage() {
    Thread {
        val text = getTextFromNetwork()
        val message = handler.obtainMessage()
        val bundle = Bundle()
        bundle.putString(key, text)
        message.data = bundle
        messageHandler.sendMessage(message)
    }.start()
}
```

You can also send empty messages with an integer value using `sendEmptyMessage(int` ie. 1 for background task succeeded and 0 for failure
```
private val emptyMessageHandler = object :
    Handler(Looper.getMainLooper()) {
        override fun handleMessage(message: Message) {
            if (message.what == 1) {
                // Update UI
            } else {
                // Show error
            }
        }
    }
private fun fetchTextWithEmptyMessage() {
    Thread {
        // get text from network
        if (failed) {
            emptyMessageHandler.sendEmptyMessage(0)
        } else {
            emptyMessageHandler.sendEmptyMessage(1)
        }
    }.start()
}
```

Disadvantages of using threads and handlers for background processing:
* Every time you need to run a background task, you should create a new thread and use runOnUiThread or a new handler to post back to the main thread
* Creating threads can consume a lot of memory and resources
* It can slow down the app
* Multiple threads make code harder to debug and test
* Code can become complicated to read and maintain
* Using threads make it difficult to handle exceptions, which can lead to crashes
* Threads is a low-level API for asynchronous programming, better to use ones that are built on top of threads like AsyncTask, or avoid it altogether with Kotlin coroutines

### Callbacks
A callback is a function that will be run when the asynchronous code has finished executing.
```
private fun fetchTextWithCallback() {
    fetchTextWithCallback { text -> 
        displayText(text)
    }
}
fun fetchTextWithCallback(onSuccess: (String) -> Unit) {
    Thread {
        val text = getTextFromNetwork()
        onSuccess(text)
    }.start()
}
```
After fetching the text in the background, the onSuccess callback will be called and will display the text on the UI thread.

Callbacks work fine for simple async tasks but they can get complicated easily, especially when nesting callback function and handling errors.

### AsyncTask
With AsyncTask, you don't have to manually handle threads
To use AsyncTask, you have to create a subclass of it with three generic types: AsyncTask<Params?, Progress?, Result?>()
Params: Type of input for AsyncTask or is void if no input needed
Progress: Used to specify the progress of the background operation or void if there's no need to track the progress
Result: Type of output of AsyncTask or is void if there's no output to be displayed

For example, to create AsyncTask to download text from a specific endpoint, your Params will be the URL(String) and Result will be the text output(String)
If you want to track the percentage of time remaining to download the text, you can use Integer for Progress
The class declaration will look like this:
``class DownloadTextAsyncTask: AsyncTask<String, Int, String>()``
And you can start AsyncTask with this:
``DownloadTextAsyncTask().execute("https://example.com")``

AsyncTask has 4 events that you can override for your background processing:
* `doInBackground`: This event specifies the actual task that will be run in the background, such as fetching/saving data to a remote server. This is the only event that you are required to override.
* `onPostExecute`: This event specifies the tasks that will be run in the UI thread after the background operation finishes, such as displaying the result
* `onPreExecute`: This event runs on the UI Thread before doing the actual task, usually displaying a progress loading indicator
* `onProgressUpdate`: This event runs in the UI thread to denote progress on the background process, such as displaying the amount of time remaining to finish the task

`onPreExecute`, `onProgressUpdate` and `onPostExecute` will run on the main thread, while `doInBackground` executes on the background thread.
```
class DownloadTextAsyncTask : AsyncTask<String, Void, String>() {
    override fun doInBackground( vararg params: String?): String? {
        val text = getTextFromNetwork(params[0] ?: "")
        return text
    }
    override fun onPostExecute(result: String?) {
        // Display UI
    }
}
```

AsyncTask can cause context leaks, missed callbacks or crashes on configuration changes.
For example, if you rotate the screen, the activity will be recreated and another AsyncTask instance will be created. The original instance won't automatically cancel and when it finished and returns to `onPostExecute`, the original activity is already gone
Using AsyncTask also makes the code more complicated and less readable
Recommended to use `java.util.concurrent` or Kotlin coroutines instead

### Executors
Executors are part of the `java.util.concurrent` package. It is a high-level Java API for managing threads.
It is an interface that has a single function `execute(Runnable)` for performing tasks.
```
val handler = Handler(Looper.getMainLooper())
private fun fetchTextWithExecutor() {
val executor = Executors.newSingleThreadExecutor()
    executor.execute {
        val text = getTextFromNetwork()
        handler.post {
            displayText()
        }
    }
}
```

ExecutorService is an executor that can do more than just execute(Runnable).
ThreadPoolExecutor is a subclass of ExecutorService that implements a thread pool that you can customise.
ExecutorService has `submit(Runnable)` and `submit(Callable)` functions, which can execute a background task. Both return a Future object that represents the result.
`Future.isDone()` can be used to check whether the executor has finished the task
`Future.get()` can be used to get the results of the task
```
val handler = Handler(Looper.getMainLooper())
private fun fetchTextWithExecutorService() {
    val executor = Executors.newSingleThreadExecutor()
    val future = executor.submit {
        displayText(getTextFromNetwork())
    }
    val result = future.get()
}
```
Although these methods still work and can still use them (except for now-deprecated AsyncTask), they are not the best method to use nowadays.

### Kotlin Coroutines
Coroutines is a Kotlin library in Android to perform asynchronous tasks. It is used to manage background tasks that return a single value.
(Meanwhile, flows are built on top of coroutines that can return multiple values)
Android Jetpack libraries such as Lifecycle, WorkManager and Room-KTX include support for coroutines. Other Android libraries such as Retrofit, Ktor and Coil provide first-class support for Kotlin coroutines
With Kotlin coroutines, you can write your code in a sequential way
A long-running task can be made into a `suspend` function. A `suspend` function can perform its task by suspending execution of the coroutine that it is called from without blocking the thread it is running on, so the thread can still run other coroutines or tasks.
When the suspending function is done, the coroutine in which it was called will resume execution. This may or may not be on the same thread, depending on the coroutine dispatcher and context in which the coroutine was launched. 
This makes the code easier to read, debug and test.
Coroutines follow a principle of structured concurrency.

Create a suspending function as follows:
```
suspend fun fetchText(): String { ... }
```
Then create a coroutine that will call the `fetchText()` suspending function and display the list, as follows:
```
lifecycleScope.launch(Dispatchers.IO) {
    val fetchedText = fetchText()
    withContext(Dispatchers.Main) {
        displayText(fetchedText)
    }
}
```
`lifecycleScope` is the scope with which the coroutine will run.
`launch` creates a coroutine to run in `Dispatchers.IO`, which is a thread for I/O or network operations.
The `fetchText()` function will suspend the coroutine before it starts the network request. While the coroutine is suspended, the main thread can do other work.
After getting the text, it will resume the coroutine.
`withContext(Dispatchers.Main)` will switch the coroutine context to the main thread, where the displayText(text) function will be executed `(Dispatchers.Main)`.

### Kotlin Flows
Flow is a new Kotlin asynchronous stream library that is built on top of coroutines. A flow can emit multiple values instead of a single value and over a period of time.
Flow is ideal to use when you need to return multiple values asynchronously, such as automatic updates from your data source.
An easy way to create a flow of objects is to use the `flow{}` builder. With the `flow{}` builder function, you can add values to the stream by calling emit.

Assume we have:
```
fun getTextFromNetwork(): String { ... }
```
If we want to create a flow of each word of the text, we can do the following:
```
private fun getWords(): Flow<String> = flow {
    getTextFromNetwork().split(" ").forEach {
        delay(1000)
        emit(it)
    }
}
```

Flow does not run or emit values until the flow is collected with any terminal operators, such as `collect`, `launchIn` or `single`.
You can use `collect()` function to start the flow and process each value, as follows:
```
private suspend fun displayWords() {
    getWords().collect {
        Log.d("flow", it)
    }
}
```
As soon as `getWords()` flow emits a string, the `displayWords()` function collects the string and displays it immediately on the logs.

## Chapter 2: Diving into Kotlin Coroutines
### Creating coroutines in Android
A simple coroutine looks like:
```
CoroutineScope(Dispatchers.IO).launch {
    performTask()
    ...
}
```
It has 4 parts: `CoroutineScope`, `Dispatchers.launch`, `launch` and the lambda function that will be executed by the coroutine.
An instance of `CoroutineScope` was created for the coroutine's scope.
`Dispatchers.IO` is the dispatcher that will specify that this coroutine will run on the I/O dispatcher, for I/O operations such as networking, database and file processing.
`launch` is the coroutine builder that creates the coroutine.
The following diagram summarises the parts of a coroutine:
`CoroutineScope[CoroutineContext[Job, CoroutineDispatcher, CoroutineName, CoroutineExceptionHandler]]`

Imagine an Android application that displays the list of movies that are currently playing in cinemas
If you are using Retrofit 2.6.0 or above, you can mark the endpoint function as a suspending function with suspend:
```
@GET("movie/now_playing")
suspend fun getMovies() : List<Movies>
```
Then, you can create a coroutine that will call the `getMovies` suspending function and display the list:
```
CoroutineScope(Dispatchers.IO).launch {
    val movies = movieService.getMovies()
    withContext(Dispatchers.Main) {
        displayMovies(movies)
    }
}
```
This will create a coroutine that fetches the movies in the background. The `withContext` call will change the context of the coroutine to use `Dispatchers.Main` to display the fetched movies in the main thread.

If you are using Room-KTX 2.1 or above, you can add the suspend keyword to your Data Access Object (DAO) functions so that the query or operation can be executed on the background thread and the result will be posted on the main thread.
```
@Dao
interface MovieDao {
    @Query("SELECT * from movies")
    suspend fun getMovies(): List<Movies>
    ...
}
```
This will make the `getMovies` query a suspending function. When you call this function, Room-KTX internally executes the query on a background thread. The results will be displayed on the main thread without freezing the app.

When you create a coroutine inside another coroutine, the new coroutine becomes the child of the original coroutine.
```
CoroutineScope(Dispatchers.IO).launch {
    performTask1()
    launch {
        performTask2()
    }
}
```
The second coroutine that was launched with `performTask2` was created using the CoroutineScope of the parent coroutine.

### Coroutine builders
Coroutine builders are the functions that you can use to create coroutines. To create a coroutine, you can use the following Kotlin coroutine builders:
* launch
* async
* runBlocking

`async` and `launch` need to be started on a coroutine scope. `runBlocking` doesn't need to be started from a coroutine scope.
The `launch` keyword creates a coroutine and doesn't return a value. Instead, it returns a `Job` object that represents the coroutine.
The `launch` coroutine builder is ideal to use when you want to run a task and then forget about it (the result of the operation is not needed).
```
class MainActivity : AppCompatActivity() {
    val scope = MainScope()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val progressBar = findViewById<ProgressBar>(R.id.progressBar)
        scope.launch {
            delay(1000)
            progressBar.isVisible = true
        }
    }
}
```
Once the activity has been created, a coroutine will be launched. This coroutine will call the `delay` suspending function to delay the coroutine for a second, resume and display the progress bar.

On the other hand, the `async` builder is similar to `launch` but it returns value: a `Deferred` object. You can get this value later with the `await` function.
The `async` builder should be used when you want to execute a task and want to get the output of the task.
```
class MainActivity : AppCompatActivity() {
    val scope = MainScope()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val textView = findViewById<TextView>(R.id.textView)
        scope.launch {
            val text = async {
                getText()
            }
            delay(1000)
            textView.text = text.await()
        }
    }
}
```

`runBlocking` starts a new coroutine and blocks the current thread until the task has been executed. This is useful for cases when you need to block the thread. Creating unit tests is one use case
```
class MainActivity : AppCompatActivity() {
    val scope = MainScope()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val progressBar = findViewById<ProgressBar>(R.id.progressBar)
        runBlocking {
            delay(2000)
            progressBar.isVisible = true
        }
    }
}
```

### Coroutine scopes
`CoroutineScope` defines the lifecycle of the coroutines created from it, from its start to its end. If you cancel a scope, it will cancel all the coroutines it created.
Coroutines follow the principle of structured concurrency.
`launch` and `async` coroutine builders are extension functions from `CoroutineScope` for creating coroutines.
`MainScope` is the main `CoroutineScope` for the main thread, which uses `Dispatchers.Main` for its coroutine. It is normally used for creating coroutines that will update user interface.

You can also create a `CoroutineScope` instead of using `MainScope` by creating one with the `CoroutineScope` factory function. The `CoroutineScope` function requires you to pass in a coroutine context.
Dispatchers and jobs are coroutine context elements. 
Your `CoroutineScope` must have a job and a way for the coroutine to be canceled, such as when `Activity`, `Fragment` or `ViewModel` has been closed.

#### lifecycleSCope
`lifecycleScope` is a `CoroutineScope` from Jetpack's Lifecycle library that you can use to create coroutines. It is tied to the `Lifecycle` object (similar to activity or fragment), and is automatically canceled when the lifecycle is destroyed. Thus there is no need to manually cancel them.
`lifecycleScope` uses `Dispatchers.Main.immediate` for its dispatcher and a `SupervisorJob` for its job, such as `viewModelScope`.
```
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val progressBar = findViewById<ProgressBar>(R.id.progressBar)
        lifecycleScope.launch {
            progressBar.isVisible = true
        }
    }
}
```
To change the dispatcher that the coroutine will use, you can pass in a dispatcher when using the launch and async coroutine builders, ie. `lifecycleScope.launch(Dispatchers.IO){ ... }`

Aside from `launch`, `lifecycleScope` has additional coroutine builders, depending on the life cycle's state: 
* launchWhenCreated - launches the coroutine when the lifecycle is created
* launchWhenStarted - launches the coroutine when the lifecycle is started
* launchWhenResumed - launches the coroutine when the lifecycle is resumed

#### viewModelScope
`viewModelScope` is the ViewModel's default `CoroutineScope` for creating coroutines. It is ideal to use if you need to do a long-running task from ViewModel. This scope and all running jobs are automatically cancelled when ViewModel is cleared.
It also uses `Dispatchers.Main.immediate` for its dispatcher and uses a `SupervisorJob` for the job. `SupervisorJob` allows its children to fail independently of each other.
```
class MovieViewModel: ViewModel() {
    init {
        viewModelScope.launch {
            fetchMovies()
        }
    }
}
```
Similarly, you can pass in a dispatcher when using the coroutine builders.

#### coroutineScope{} and supervisorScope{}
`coroutineScope{}` suspending builder allows you to create a `CoroutineScope` with the coroutine context from its outer scope. This calls the code block inside and does not complete until everything is done.
```
private suspend fun fetchaAndDisplay() = coroutineScope {
    launch {
        val movies = fetchMovies()
        displayMovies(movies)
    }
    launch {
        val shows = fetchShows()
        displayShows(shows)
    }
}
```
This creates two children coroutines, one for movies and one for shows.
When a child coroutine fails, it will cancel the parent coroutine and the sibling coroutines. If this is not the desired result, you can use `supervisorScope{}` instead.

`supervisorScope{}` builder is similar but the coroutine's Scope has a `SupervisorJob`, so its children will fail independently of each other.
```
private suspend fun fetchaAndDisplay() = supervisorScope {
    launch {
        val movies = fetchMovies()
        displayMovies(movies)
    }
    launch {
        val shows = fetchShows()
        displayShows(shows)
    }
}
```

#### GlobalScope
`GlobalScope` is a special `CoroutineScope` that is not tied to an object or a job. It should only be used in cases when you must run a task or tasks that will always be active while the application is alive.
For all other cases in Android, it is recommended to use viewModelScope, lifecycleScope or a custom coroutine scope.

### Coroutine dispatchers
Coroutines have a context which includes the coroutine dispatcher. Dispatchers specify what thread the coroutine will use to perform the task.
* Dispatchers.Main: Run on Android's main thread, usually for updates to user interface. Dispatchers.Main.immediate is used to immediately execute the coroutine in the main thread.
* Dispatchers.IO: Designed for networking operations and for reading or writing to files or databases
* Dispatchers.Default: This is used for CPU-intensive work, such as complicated computations or processing text, images or videos. It is also the default dispatcher if not specified.
* Dispatchers.Unconfined: This is a special dispatcher that is not confined to any specific threads. It executes the coroutine in the current thread and resumes it in whatever thread that is used by the suspending function.

To change the context of your coroutine, you can use `withContext` function for the code that you want to use a different thread with.

### Coroutine contexts
Each coroutine runs in a coroutine context.A coroutine context is a collection of elements for the coroutines that specifies how the coroutine should run.
Some `CoroutineContext` elements that you can use are:
* CoroutineDispatcher
* Job
* CoroutineName
* CoroutineExceptionHandler
The main `CoroutineContext` elements are dispatchers and jobs. Dispatchers specify the thread where the coroutine runs, while the job allows you to manage the coroutine's task.
Jobs allow you to manage the lifecycle of the coroutine, from the creation of the coroutine to the completion of the task. You can use this job to cancel the coroutine itself.

`CoroutineName` can be used to set a string to name a coroutine. This name can be useful for debugging purposes.
```
val scope = CoroutineScope(Dispatchers.IO)
scope.launch(CoroutineName("IOCoroutine") { performTask() }
```

You can use operators such as the + symbol to combine context elements to create a new CoroutineContext ie. `val context = Dispatchers.Main + Job()`

### Coroutine jobs
Jobs are used to manage the coroutine's tasks and its life cycle. Jobs can be canceled or joined together.
The `launch` coroutine builder creates a new job, while the `async` coroutine builder returns a `Deferred<T>` object. `Deferred` is a `Job` object - a job that has a result.
To access the job from the coroutine, you can set it to a variable:
```
val job = viewModelScope.launch(Dispatchers.IO) { ... }
```
A job can have children jobs, which can be accessed with the `children` property.
```
val job1 = viewModelScope.launch(Dispatchers.IO) {
    val movies = fetchMovies()
    val job2 = launch { ... }
}
```
job2 becomes a child of job1, which is the parent. This means that job2 will inherit the coroutine context of the parent, though you can also change it.

Using a job also allows you to create a coroutine that you can start later instead of immediately running by default. To do this, use `CoroutineStart.LAZY` as the value of the `start` parameter in you coroutine builder and assign the result to a `Job` variable. Later, you can then use the `start()` function to run the coroutine:
```
val lazyJob = viewModelScope.launch(start=CoroutineStart.LAZY) {
    delay(1000)
    ...
}
...
lazyJob.start()
```
This creates a lazy coroutine, which can be called whenever using `lazyJob.start()`.

You can also use the `join()` suspending function to wait for the job to be completed before continuing with another job or task.
```
viewModelScope.launch {
    val job1 = launch { showProgressBar() }
    ...
    job1.join()
    ...
    val job2 = launch { fetchMovies() }
}
```
job1 will be run first and job2 won't be executed until job1 is finished.

### Coroutine job states
A job has the following states:
* New
* Active
* Completing
* Completed
* Cancelling
* Cancelled

## Chapter 3: Handling Coroutine Cancellations and Exceptions
### Cancelling coroutines
If using `viewModelScope` or `lifecycleScope`, you can create coroutines without manually handling the cancellation. When ViewModel is cleared, `viewModelScope` is automatically cancelled while `lifecycleScope` is automatically cancelled when the lifecycle is destroyed. But if you create your own coroutine scope, you must add the cancellation.
Using the `job` object that is returned from `launch`, you can call the `cancel()` function to cancel the coroutine:
```
class MovieViewModel: ViewModel() {
    init {
        viewModelScope.launch {
            val job = launch { fetchMovies() }
            ...
            job.cancel()
        }
    }
}
```
After cancelling the job, you may want to wait for the cancellation to be finished before continuing to the next task to avoid race conditions. You can do that be calling the `join` function after calling the `call` function:
```
class MovieViewModel: ViewModel() {
    init {
        viewModelScope.launch {
            val job = launch { fetchMovies() }
            ...
            job.cancel()
            job.join()
            hideProgressBar()
        }
    }
}
```
Or you can use the `Job.cancelAndJoin()` extension function which is the same as the above

If your coroutine scope has multiple coroutines and you need to cancel all of them, you can use the `cancel` function from the coroutine scope instead of cancelling the jobs one by one.
```
class MovieViewModel: ViewModel() {
    private val scope = CoroutineScope(Dipatchers.Main + Job())
    init {
        scope.launch {
            val job1 = launch { fetchMovies() }
            val job2 = launch { displayLoadingText() }
        }
    }
    override fun onCleared() {
        scope.cancel()
    }
}
```
However, the coroutine scope won't be able to launch new coroutines after you called the `cancel` function on it. If you want to cancel the scope's coroutines but still want to create coroutines from the scope later, you can use `scope.coroutineContext.cancelChildren()` instead
```
class MovieViewModel: ViewModel() {
    private val scope = CoroutineScope(Dipatchers.Main + Job())
    init {
        scope.launch {
            val job1 = launch { fetchMovies() }
            val job2 = launch { displayLoadingText() }
        }
    }
    fun cancelAll() {
        scope.coroutineContext.cancelChildren()
    }
}
```

Cancelling a coroutine will throw `CancellationException`, a special exception that indicates the coroutine was cancelled. This exception will not crash the application.
You can also pass a subclass of `CancellationException` to the `cancel` function to specify a different cause:
```
class MovieViewModel: Viewmodel() {
    private lateinit var movieJob: Job
    init {
        movieJob = scope.launch() {
            fetchMovies()
        }
    }
    fun stopFetching() {
        movieJob.cancel(CancellationException("Cancelled by user"))
    }
}
```

When you cancel a coroutine, its job's state will change to Cancelling. It will not automatically go to Cancelled state and cancel the coroutine. The coroutine can continue to run even after the cancellation, unless your code makes it stop running.
Your coroutine needs to cooperate to be cancellable. The coroutine should handle cancellations as quickly as possible.
One way to make your coroutine cancellable is toc heck whether the coroutine job is active(still running or completing) or not by using `isActive`. The value of `isActive` will become false once the coroutine job changes its state to Cancelling, Cancelled or Completed.
You can make your coroutine cancellable by:
* Perform tasks while `isActive` is true
* Perform tasks only if `isActive` is true
* Return or throw an exception if `isActive` is false
You can also use `Job.ensureActive()`, which will check whether the coroutine job is active, else it will throw `CancellationException`
```
class SensorActivity : AppCompatActivity() {
    private val scope = CoroutineScope(Dispatchers.IO)
    private late init var job: Job
    
    private fun processSensorData() {
        job = scope.launch {
            if (isActive) {
                val data = fetchSensorData()
                saveData(data)
            }
        }
    }
    fun stopProcessingData() {
        job.cancel()
    }
}
```

Another way to make your coroutine code cancellable is to use suspending functions such as `yield` or `delay`. `yield` yields a thread (or a thread pool) of the current coroutine dispatcher to other coroutines to run.
These functions already check for cancellation and stop the execution or throw `CancellationException`, hence there is no need to manually check for cancellation when you are using them in your coroutines.
```
class SensorActivity: AppCompatActivity() {
    private val scope = CoroutineScope(Dispatchers.IO)
    private lateinit var job: Job
    override fun onCreate(savedInstanceState: Bundle?) {
        ...
        processSensorData()
    }
    private fun processData() {
        job = scope.launch {
            delay(1000)
            val data = fetchSensorData()
            saveData(data)
        }
    }
    fun stopProcessingData() {
        job.cancel()
    }
    ...
}
```
The `delay` suspending function will check whether the coroutine job is cancelled and will throw `CancellationException` if it is making your coroutine cancellable.

### Managing coroutine timeouts
Use `withTimeout` to set a timeout for the coroutine. If the timeout is exceeded, it will throw `TimeOutCancellationException`
```
class MovieViewModel: ViewModel() {
    init {
        viewModelScope.launch {
            val job = launch {
                withTimeout(5000) {
                    fetchMovies()
                }
            }
        }
    }
}
```

Use `withTimeoutOrNull` which is similar, but will return null if the timeout was exceeded.
```
class MovieViewModel: ViewModel() {
    init {
        viewModelScope.launch {
            val job = async {
                fetchMovies()
            }
            val movies = withTimeoutOrNull(5000) {
                job.await()
            }
        }
    }
}
```

### Catching exceptions in coroutines
Use try-catch 
* If using `launch` coroutine builder, try the suspending function, and catch the exception
* If using `async` coroutine builder, try `job.await()` and catch the exception

If the exception of the coroutine is a subclass of `CancellationException` ie. `TimeoutCancellationException`, the exception will not be transmitted to the parent

When handling coroutine exceptions, you can use a single place to handle these exceptions with `CoroutineExceptionHandler`. It is a coroutine context element that you can add to your coroutine to handle uncaught exceptions
```
class MovieViewModel: ViewModel() {
    private val exceptionHandler = CoroutineExceptionHandler { _, exception -> 
        Log.e("MovieViewModel", exception.message.toString())
    }
    private val scope = CoroutineScope(exceptionHandler)
    ...
}
```

## Chapter 4: Testing Kotlin Coroutines
### Setting up an Android project for testing coroutines
Frameworks needed:
* JUnit 4 - unit testing framework for Java
* Mockito - most popular Java mocking library to create mock objects for tests (can also use Mockito-Kotlin which contains helper functions to make your code more Kotlin-like)
* androidx.arch.core:core-testing - to test Jetpack components such as LiveData
* kotlinx-coroutines.test - contains utility classes to make testing coroutines easier and more efficient

### Unit testing suspending functions
Use the `runBlocking` coroutine builder and call the suspending function from there
For example, if you have a MovieRepository class, which has a suspending function called fetchMovies:
```
class MovieRepository (private val movieService: MovieService) {
    private val movieLiveData = MutableLiveData<List<Movie>>()
    fun fetchMovies() {
        val movies = movieService.getMovies()
        movieLiveData.postValue(movies.result)
    }
}
```
To create a test for fetchMovies, you can use `runBlocking` like this:
```
class MovieRepositoryTest {
    @Test
    fun fetchMovies() {
        runBlocking {
            val movieLiveData = movieRepository.fetchMovies()
            assertEquals(movieLiveData.value, movies)
        }
    }
}
```

`runBlocking` might be slow due to delays in the code. Can use `runTest` coroutine builder instead, which is essentially the same except it runs the suspending function immediately and without delays

### Testing coroutines
For coroutines launched using `Dispatchers.Main`, your unit tests will fail with IllegalStateException because it uses `Looper.getMainLooper()` which is the application's main thread. The main looper is not available for local unit tests.
To make your tests work, you must use `Dispatchers.setMain` extension function to change the main dispatcher.
```
@Before
fun setUp() {
    Dispatchers.setMain(UnconfinedTestDispatcher())
}
```
This will change all subsequent uses of `Dispatchers.Main`. After the test, you must change the main dispatcher back with a call to `Dispatchers.resetMain()`
```
@After
fun tearDown() {
    Dispatchers.resetMain()
}
```

You can make a custom JUnit rule to avoid copying and pasting this code in each test class. Ensure that this rule is in the root folder of your test source set.
```
@ExperimentalCoroutineApi
class TestCoroutineRule(val dispatcher: TestDispatcher = UnconfinedTestDispatcher()):
    TestWatcher() {
        override fun starting(description: Description?) {
            super.starting(description)
            Dispatchers.setMain(dispatcher)
        }
        override fun finished(description: Description?) {
            super.finished(description)
            Dispatchers.resetMain()
        }
    }
```
You can then use this `TestCoroutineRule` in your test classes by adding the `@get:Rule` annotation.
```
@ExperimentalCoroutineApi
class MovieRepositoryTest {
    @get:Rule
    var coroutineRule = TestCoroutineRule()
    ...
}
```

When testing coroutines, you must replace your coroutine dispatchers with a `TestDispatcher` for testing. Your code should have a way to change the dispatcher that will be used.
There are two available implementations of `TestDispatcher` in the `kotlinx-coroutines-test` library:
* StandardTestDispatcher - does not run coroutines automatically, giving you full control over execution order
* UnconfinedTestDispatcher - runs coroutines automatically, offers no control over the order in which the coroutines are launched
Both have constructor properties: scheduler for TestCoroutineScheduler, and name for identifying the dispatcher. If you do not specify the scheduler, TestDispatcher will create a TestCoroutineScheduler by default
TestCoroutineScheduler of the StandardTestDispatcher controls the execution of the coroutine, and has three functions:
* runCurrent() - Runs the tasks that are scheduled until the current virtual time
* advanceUntilIdle() - Runs all pending tasks
* advanceTimeBy(milliseconds) - Runs pending tasks until current virtual advances by the specified milliseconds

The `runTest` coroutine builder creates a coroutine with a scope of `TestScope`, which has a `TestCoroutineScheduler(testScheduler)` to control the execution of tasks
Using `runTest` with a `TestDispatcher` allows you to test cases when there are time delays in the coroutine and you want to test a line of code before moving on to the next ones.
For example, if ViewModel has a `loading` Boolean variable that is `true` before a network operation and then reset to `false` afterwards, the test for the `loading` variable looks like this:
```
@Test
fun loading() {
    val dispatcher = StandardTestDispatcher()
    runTest() {
        val viewModel = MovieViewModel(dispatcher)
        viewModel.fetchMovies()
        dispatcher.scheduler.advanceUntiLIdle()
        assertEquals(false, viewModel.loading.value)
    }
}
```

## Chapter 5: Using Kotlin Flows
Kotlin Flow is a new asynchronous stream library built on top of Kotlin coroutines. A flow can emit multiple values over a length of time instead of just a single value. You can use Flows for streams of data such as real-time location, sensor readings and live database values.

### Using Flows in Android
Flows emit multiple values of the same type one at a time ie. `Flow<String` is a flow that emits string values.
For example, if your Android application uses a Data Access Object to display a list of movies, it might look like this:
```
@Dao
interface MovieDao {
    @Query("SELECT * FROM movies")
    fun getMovies(): List<Movie>
}
```
However, this only fetches the list of movies once, after calling `getMovies`. You may want the app to automatically update the list of movies whenever the movie database changes. To do so:
```
@Dao
interface MovieDao {
    @Query("SELECT * FROM movies")
    fun getMovies(): Flow<List<Movie>>
}
```

A flow will only start emitting values when you call the `collect` function. The `collect` function is a suspending function, so you should call it from a coroutine or another suspending function.
```
lifecycleScope.launch {
    viewModel.fetchMovies().collect { movie -> 
        Log.d("movies", "${movie.title}")
    }
}
```

The collection of the flow occurs in `CoroutineContext` of the parent coroutine. To change the `CoroutineContext` where the Flow is run, you can use the `flowOn()` function.
```
lifecycleScope.launch {
    viewModel.fetchMovies().flowOn(Dispatchers.IO).collect { movie ->
        Log.d("movies", "${movie.title}")
    }
}
```
`flowOn` will only change the preceding functions or operators and not the ones after you called it.

You can collect Flow in the Fragment or Activity classes to display the data in the UI. If the UI goes to the background, your Flow will keep on collecting the data. To prevent memory leaks and avoid wasting resources, your app must not continue collecting the Flow.
To safely collect flows in the Android UI layer, you would need to handle the lifecycle changes yourself. You can use `Lifecycle.repeatOnLifecycle` and `Flow.flowWithLifecycle`.
`Lifecycle.repeatOnLifecycle(state, block)` suspends the parent coroutine until the lifecycle is destroyed and executes the suspending block of code when the lifecycle is at least in state you set. When the lifecycle moves out of the state, it will stop the Flow and restart it when the lifecycle moves back to the state.
It is recommended to call it on the activity's `onCreate` or on the fragment's `onViewCreated` functions.
```
class MainActivity : AppCompatActivity() {
    ...
    override fun onCreate(savedInstanceState: Bundle?) {
        ...
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.fetchMovies()
                    .collect { movie -> 
                        Log.d("movies, "${movie.title}")
                    }
            }
        }
    }
}
```
You can also use `Lifecycle.repeatOnLifecycle` to collect more than one Flow by collecting them in separate `launch` coroutine builders ie.
```
class MainActivity : AppCompatActivity() {
    ...
    override fun onCreate(savedInstanceState: Bundle?) {
        ...
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.fetchMovies()
                        .collect { movie -> 
                            Log.d("movies, "${movie.title}")
                        }
                }
                launch {
                    viewModel.fetchTVShows()
                        .collect { show -> 
                            Log.d("tv shows, "${show.title}")
                        }
                }
            }
        }
    }
}
```

If you only have one Flow to collect, you can use `Flow.flowWithLifecycle`. This emits values from the upstream Flow when the lifecycle is at least in the state you set. It uses `Lifecycle.repeatOnLifecycle` internally.
```
class MainActivity : AppCompatActivity() {
    ...
    override fun onCreate(savedInstanceState: Bundle?) {
        ...
        lifecycleScope.launch {
            viewModel.fetchMovies()
                .flowWithLifecycle(lifecycle, Lifecycle.State.STARTED)
                .collect { movie ->
                    Log.d("movies", "${movie.title}")
                }
        }
    }
}
```

### Creating Flows with Flow builders
Flow builders:
* flow { }
* flowOf()
* asFlow()

The `flow` builder function creates a new Flow from a suspendable lambda block. Inside the block, you can send values using the `emit` function.
```
class MovieViewModel : ViewModel() {
    fun fetchMovieTitles(): Flow<String> = flow {
        val movies = fetchMoviesFromNetwork()
        movies.forEach { movie ->
            emit(movie.title)
        }
    }
    private fun fetchMoviesFromNetwork(): List<Movie> { ... }
}
```

With the `flowOf` function, you can create a Flow that produces the specified value or vararg values.
```
class MovieViewModel : ViewModel() {
    fun fetchTop3Titles(): Flow<List<Movie>> {
        val movies = fetchMoviesFromNetwork().sortedBy { it.popularity }
        return flowOf(movies[0].title, movies[1].title, movies[2].title)
    }
}
```

The `asFlow` extension function allows you to convert a type into a Flow. You can use this on sequences, arrays, ranges, collections and functional types.
```
class MovieViewModel : ViewModel() {
    private fun fetchMovieIds(): Flow<Int> {
        val movies: List<Movie> = fetchMoviesFromNetwork()
        return movies.map { it.id }.asFlow()
    }
}
```

### Using operators with Flows
Kotlin Flow has built-in operators that you can use with Flows. You can collect flows with terminal operators and transform Flows with Intermediate operators.

#### Collecting Flows with terminal operators
* toList - Collects Flow and converts it to list
* toSet - converts to set
* toCollection - converts to collection
* count - returns number of elements in the Flow
* first / firstOrNull - returns Flow's first element or throws Exception / null if empty
* last/ lastOrNull - last element
* single / singleOrNull - returns single element emitted or throws Exception / null if empty or more than one value
* reduce - Applies a function to each item emitted, starting from the first element and returns the accumulated result
* fold - Applies a function to each item emitted, started from the initial value set and returns the accumulated result

```
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                val topMovie = viewModel.fetchMovies().firstOrNull()
                displayMovie(topMovie)
            }
        }
    }
}
```
`firstOrNull` was used on the Flow to get the first item, or null if the Flow is empty.

#### Transforming Flows with Intermediate operators
Intermediate operators allow you to modify a Flow and return a new one. You can chain various operators and they will be applied sequentially. 
* filter - Returns a Flow that selects only the values from the Flow that meet the condition
* filterNot - Returns a Flow that selects only the values that do not meet the condition
* filterNotNull - values that are not null
* filterIsInstance - values that are instances of the type you specified
* map - Returns a Flow that includes values from the Flow transformed with the operation you specified
* mapNotNull - Like map but only includes values that are not null
* withIndex - Returns a Flow that converts each value to an IndexedValue containing the index of the value and the value itself
* onEach - Returns a Flow that performs the specified action on each value before they are emitted
* runningReduce - Returns a Flow containing the accumulated values resulting from running the operation specified sequentially, starting with the first element
* runningFold - Returns a Flow containing the accumulated values resulting from running the operation specified sequentially, starting with the initial value set
* scan - Like runningFold
* transform - Apply custom or complex operations, can emit values into the new Flow by calling the emit function with the value to send
* drop - Returns a Flow that ignores the first x elements
* dropWhile - ignores first elements that meet the condition specified
* take - Returns a flow that contains the first x elements
* takeWhile - includes the first elements that meet the condition specified

```
class MovieViewModel : ViewModel() {
    fun fetchTopMovies(): Flow<Movie> {
        return fetchMoviesFlow().transform {
            if (it.popularity > 0.5f) emit (it)
        }
    }
}
```

### Buffering and combining flows
Buffering allows Flow with long-running tasks to run independently and avoid race conditions. Combining allows you to join different sources of Flows before processing or displaying them on the screen.

#### Buffering Kotlin Flows
Buffering allows you to run data emission in parallel to collection. Emitting and collecting data with Flow run sequentially. When a new value is emitted, it will be collected. Emission of a new value can only happen once the previous data has been collected.
With buffering, you can make a Flow's emission and collection of data run in parallel. There are three operators to use to buffer Flows:
* buffer
* conflate
* collectLatest

`buffer()` allows the Flow to emit values while the data is still being collected. The emission and collection of data are run in separate coroutines, so it runs in parallel.
```
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.fetchMovies()
                    .buffer()
                    .collect { movie ->
                        processMovie(movie)
                    }
            }
        }
    }
}
```
The `buffer` operator was added before calling `collect`. If the `processMovie(movie)` function in the collection takes longer, the Flow will emit and buffer the values before they are collected and processed.

`conflate` is similar to `buffer` except the collector will only process the latest value emitted after the previous value has been processed. It will ignore theother values previously emitted.
```
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.getTopMovie()
                    .conflate()
                    .collect { movie ->
                        processMovie(movie)
                    }
            }
        }
    }
}
```
Adding the `conflate` operator will allow us to only process the latest value from the Flow and call `processMovie` on that value.

`collectLatest(action)` is a terminal operator that will collect the Flow the same way as `collect`, but whenever a new value is emitted, it will restart the action and use this new value.
```
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.getTopMovie()
                    .collectLatest { movie ->
                        processMovie(movie)
                    }
            }
        }
    }
}
```
`collectLatest` was used instead of `collect` to collect the flow from `viewModel.getTopMovie()`. Whenever a new value is emitted by this Flow, it will restart and call displayMovie with the new value.

#### Combining Flows
If you have multiple flows and want to combine them into one, you can use the following operators:
* zip
* merge
* combine

`merge` is a top-level function that combines the elements from multiple Flows of the same type into one. 
```
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                merge(viewModel.fetchMoviesFromDb(),
                    viewModel.fetchMoviesFromNetwork())
                    .collect { movie -> processMovie(movie) }
            }
        }
    }
}
```
`merge` was used to combine the Flows from `fetchMoviesFromDb()` and `fetchMoviesFromNetwork()` before they are collected.

The `zip` operator pairs data from the first Flow to the second Flow into a new value using the function you specified. If one Flow has fewer values than the other, `zip` will end when the values of this flow have all been processed.
```
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                val userFlow = viewModel.getUsers()
                val taskFlow = viewModel.getTasks()
                userFlow.zip(taskFlow) { user, task ->
                    AssignedTask(user, task)
                }.collect { assignedTask ->
                    displayAssignedTask(assignedTask)
                }
            }
        }
    }
}
```

`combine` pairs data from the first flow to the second flow like `zip` but uses the most recent value emitted by each flow. It will continue to run as long as a Flow emits a value.
```
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                val yourMessage = viewModel.getLastMessageSent()
                val friendMessage = viewModel.getLastMessageReceived()
                yourMessage.combine(friendMessage) { yourMessage, friendMessage -> 
                    Conversation(yourMessage, friendMessage)
                }.collect { conversation -> displayConversation(conversation) }
            }
        }
    }
}
```
The `combine` function pairs the most recent value of yourMessage and friendMessage to create a Conversation object. Whenever a new value is emitted by either Flow, combine will pair the latest values and add that to the resulting Flow for collection.

### Exploring StateFlow and SharedFlow
A Flow is a cold stream of data. Flows only emit values when the values are collected. With SharedFlow and StateFlow hot streams, you can run and emit values the moment they are called and even when they have no listeners.

A SharedFlow allows you to emit values to multiple listeners. SharedFlow can be used for one-time events. The tasks that will be done by the SharedFlow will only be run once and will be shared by the listeners.
You can use MutableSharedFlow and then use the `emit` function to send values to all collectors.
```
class MovieViewModel: ViewModel() {
    private val _message = MutableSharedFlow<String>()
    val movies: SharedFlow<String> = _message.asSharedFlow()
    
    fun onError(): Flow<List<Movie>> {
        _message.emit("An error was encountered")
    }
}
```
We used the `emit` function to send the error message to the Flow's listeners.

StateFlow is SharedFlow, but it only emits the latest value to its listeners. StateFlow is initialised with a value (initial state) and keeps this state. Updating the value sends the new value to the Flow.
In Android, StateFlow can be an alternative to LiveData. You can use StateFlow for ViewModel, and your activity or fragment can then collect the value.
```
class MovieViewModel: ViewModel() {
    private val _movies = MutableStateFlow(emptyList<Movie>())
    val movies: StateFlow<List<Movie>> = _movies
    
    fun fetchMovies(): Flow<List<Movie>> {
        _movies.value = movieRepository.fetchMovies()
    }
}
```
The list of movies fetched from the repository will be set to `_movies`, which will also change StateFlow of `movies`. You can then collect StateFlow of `movies` in an activity or fragment:
```
class MainActivity : AppCompatActivity() {  
    override fun onCreate(savedInstanceState: Bundle?) {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.movies.collect { movies ->
                    displayMovies(movies)
                }
            }
        }
    }
}
```

## Chapter 6: Handling Flow Cancellations and Exceptions
### Cancelling Kotlin Flows
Like coroutines, Flows can be cancelled manually or automatically. Flow also follows the cooperative cancellation of coroutines.
Flows created using the `flow{}` builder are cancellable by default. Each `emit` call to send new values to the Flow also calls `ensureActive` internally. This checks whether the coroutine is still active, if not it will throw `CancellationException`.

All other Flows, such as `asFlow` and `flowOf` builders are not cancellable by default, and we must handle the cancellation ourselves. There is a `cancellable()` operator we can use to make it cancellable. This will add an `ensureActive` call on each emission of a new value
```
class MovieViewModel : ViewModel() {
    fun fetchMovies(): Flow<Movie> {
        return movieRepository.fetchMovies.cancellable()
    }
}
```

### Retrying tasks with Flow
When performing long-running tasks, such as a network call, sometimes it is necessary to try the call again. With Kotlin Flows, we have the `retry` and `retryWhen` operators that we can use to try Flows automatically.
The `retry` operator allows you to set a `Long retries` as the maximum number of times the Flow will retry. You can also set a predicate condition, a code block that will retry the Flow when it returns true. The predicate has a `Throwable` parameter representing the exception that occurred and you can use that to check whether you want to do the retry ot not.
```
class MovieViewModel : ViewModel() {
    fun favouriteMovie(id: Int) =
        movieRepository.favouriteMovie(id).retry(3) { cause -> cause is IOException }
}
```
If you do not pass a value for the entries, the default of `Long.MAX_VALUE` will be used. 

With the `retryWhen` operator, we can also emit a value to the Flow, which we can use to represent the retry attempt or a value. We can then display this value on the screen or process it.
```
class MovieViewModel : ViewModel() {
    fun getTopMovieTitle(): Flow<String> {
        return movieRepository.getTopMovieTitle(id)
            .retryWhen { cause, attempt ->
                emit("Fetching title again...")
                attempt < 3 && cause is IOException)
            }
    }
}
```

### Catching exceptions in Flows
Exceptions can happen in Flows during the collection of values or when using any operators on a Flow. We can handle using a try-catch block.
We can also use the `catch` operator to emit a new value to represent the error or for use as a fallback value instead, such as an empty list.
```
lifecycleScope.launch {
    repeatOnLifecycle(Lifecycle.State.STARTED) {
        viewModel.getTopMovieTitle()
            .catch { emit("No movie fetched") }
            .collect { title -> displayTitle(title) }
    }
}
```
As the `catch` operator only handles exceptions in the upstream Flow, an exception that happens during the `collect{}` call won't be caught. You can move the collection code to an `onEach` operator, add the `catch` operator after it, and use `collect()` to start the collection.
```
lifecycleScope.launch {
    repeatOnLifecycle(Lifecycle.State.STARTED) {
        viewModel.fetchMovies()
            .onEach { movie -> processMovie(movie) }
            .catch { exception -> handleError(exception) }
            .collect()
    }
}
```

### Handling Flow completion
We can add code to perform additional tasks after our Flows have completed. A flow is completed after it is cancelled, or when the last element has been emitted.
To add a listener in your Flow when it has completed, you can use the `onCompletion` operator and add the code block that will run when the Flow completes. A common usage is to hide the progress bar in your UI when the Flow has completed.
```
lifecycleScope.launch {
    repeatOnLifeycle(Lifecycle.State.STARTED) [
        viewModel.fetchMovies()
            .onStart { progressBar.isVisible = true }
            .onEach { movie -> processMovie(movie) }
            .onCompletion { progressBar.isVisible = false }
            .catch { exception -> handleError(exception) }
            .collect()
    }
}
```

You can also emit values, such as initial and final values in `onStart` and `onCompletion`.
```
lifecycleScope.launch {
    repeatOnLifecycle(Lifecycle.State.STARTED) {
        viewModel.getTopMovieTitle()
            .onStart { emit("Loading...") }
            .catch { emit("No movie fetched") }
            .collect { title -> displayTitle(title) }
    }
}
```

The `onCompletion` code block also has a nullable `Throwable` that corresponds to the exception thrown by the Flow. The exception will not handle by itself, so you will need to use `catch` or `try-catch`.
```
viewModel.getTopMovieTitle()
    .onCompletion { cause ->
        progressBar.isVisible = false
        if (cause != null) displayError(cause)
    }
    .catch { emit("No movie fetched") }
    .collect { title -> displayTitle(title) }
```

## Testing Kotlin Flows