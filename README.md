Reactive-java-course

Oleh Dokuka - Reactive Hardcore: How to Build Your Publisher
Rep - https://github.com/CollaborationInEncapsulation/reactive-hardcore
Video with explanations on youtube - https://www.youtube.com/watch?v=qmuNAWKNJWs

Oleh tries to implement spec of Rx-programming example-by-example.
1. Example1 is about ordering of calling methods in 
   Here Oleh says about right order: 
       - in Publisher create Subscription
       - send to Subscriber -> Subscription: subscriber.onSubscribe(subscription);
       - send data: subscriber.onNext(data);
       - says to Subscriber that is finish: subscriber.onComplete();
2. Example2 (10:20) is about back pressure (backpressure). Backpressure - it's control of thread of data. Problem of 
    example1 is that data was came when we even didn't ask.
3. Example3 (12:30) is about checking null.
4. Example4 (13:50) is about recursion. Oleh uses pattern - work in progress.