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