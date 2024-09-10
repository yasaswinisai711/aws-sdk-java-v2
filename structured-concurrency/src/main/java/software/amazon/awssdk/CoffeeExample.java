package software.amazon.awssdk;

import com.sun.management.HotSpotDiagnosticMXBean;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.time.Duration;
import java.util.concurrent.ExecutionException;

import static java.util.concurrent.StructuredTaskScope.Subtask;
import static java.util.concurrent.StructuredTaskScope.ShutdownOnFailure;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import javax.management.MBeanServer;

/**
 *
 */
public class CoffeeExample {
    private static final boolean kettleBroken = false;

    private record CoffeeRecipe(int millisOfWater, int gramsOfCoffee) {
    }

    private record CupOfCoffee(double strength, int size) {
    }

    int fillKettle() throws InterruptedException {
        System.out.println("[fill] filling kettle");
        int amountOfWaterInMl = 1000;
        TimeUnit.SECONDS.sleep(3);
        System.out.println("[fill] done filling kettle");
        return amountOfWaterInMl;
    }

    int boilWater(int milliliters) throws InterruptedException {
        System.out.printf("[boil] starting to boil %s ml of water%n", milliliters);
        if (kettleBroken) {
            throw new RuntimeException("Kettle Broken :(");
        }
        for (int i = 0; i < 10; i++) {
            System.out.printf("[boil] getting hotter %s%n", ".".repeat(i));
            TimeUnit.SECONDS.sleep(1);
        }
        System.out.println("[boil] WATER BOILING!!!");
        return milliliters;
    }

    int grindCoffee(Duration grindTime) throws InterruptedException {
        int gramsOfCoffee = 70;
        System.out.printf("[grind] grinding %s grams of coffee beans%n", gramsOfCoffee);
        for (int j = 0; j < grindTime.getSeconds(); j++) {
            System.out.printf("[grind] grinding %s%n", ".".repeat(j));
            TimeUnit.SECONDS.sleep(1);
        }
        System.out.printf("[grind] %s grams of coffee beans done grinding%n", gramsOfCoffee);
        return gramsOfCoffee;
    }

    CupOfCoffee pourOver(CoffeeRecipe recipe) throws InterruptedException {
        System.out.println("[brew] Coffee pour over");
        for (int i = 0; i < 10; i++) {
            System.out.printf("[brew] brewing%s%n", ".".repeat(i));
            TimeUnit.SECONDS.sleep(1);
        }
        System.out.println("[brew] Done pour over");
        return new CupOfCoffee(recipe.gramsOfCoffee() / (double) recipe.millisOfWater(), recipe.millisOfWater());
    }

    Void cleanKitchen() throws InterruptedException {
        System.out.println("[clean] cleaning kitchen");
        TimeUnit.SECONDS.sleep(8);
        System.out.println("[clean] done cleaning");
        return null;
    }

    CupOfCoffee brewCoffee() throws Exception {
        System.out.println("[main] Starting brew");

        try (ShutdownOnFailure scope = new ShutdownOnFailure()) {
            // run asynchronously:
            //    - fillKettle followed by boilWater (synchronously)
            //    - grindCoffee
            //    - dump thread
            Subtask<Integer> waterTask = scope.fork(() -> {
                // synchronous call
                int water = fillKettle();
                return boilWater(water);
            });
            Subtask<Integer> beanTask = scope.fork(() -> grindCoffee(Duration.ofSeconds(5)));

            scope.fork(() -> dumpThreadAfter(
                Duration.ofSeconds(2),
                "/Users/olapplin/Develop/GitHub/aws-sdk-java-v2/structured-concurrency/thread-dump.json"));

            // Wait for both forked tasks to finish while they run in parallel.
            // Because ShutdownOnFailure is used, automatically shutdown the scope if any subtask fails.
            scope.join()

                 // Only available with the ShutdownOnFailure API.
                 // Will throw java.util.concurrent.ExecutionException if any subtask failed (threw an exception),
                 // with the cause the exception thrown in the task
                 .throwIfFailed();

            // Alternatively, Subtask#state() can be used to check the state of individual tasks
            System.out.printf("[main] water task was %s%n", waterTask.state());
            System.out.printf("[main] bean task was %s%n", beanTask.state());

            // Note: the JEP recommends only the shutdown policy to access Subtask API, and instead
            // Use

            // if any task failed before, get() would throw a IllegalStateException
            int water = waterTask.get();
            int coffeeGrind = beanTask.get();

            CoffeeRecipe coffeeRecipe = new CoffeeRecipe(water, coffeeGrind);

            // run asynchronously in parallel pourOver and cleanKitchen
            Supplier<CupOfCoffee> pourOverTask = scope.fork(() -> pourOver(coffeeRecipe));
            scope.fork(this::cleanKitchen);

            // wait for both to finish
            scope.join();

            scope.throwIfFailed();
            return pourOverTask.get();
        } catch (ExecutionException e) {
            System.err.println(e.getMessage());
            return new CupOfCoffee(0, 0);
        }
    }

    private static class CoffeeBrewException extends RuntimeException {
        public CoffeeBrewException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    public static void main(String[] args) throws Exception {
        CoffeeExample coffeeExample = new CoffeeExample();
        CupOfCoffee cup = coffeeExample.brewCoffee();
        System.out.println(cup.toString());
    }

    public Void dumpThreadAfter(Duration waitTime, String filePath) throws Exception {
        TimeUnit.MILLISECONDS.sleep(waitTime.toMillis());
        System.out.printf("[dump] dumping thread to %s%n", filePath);
        MBeanServer server = ManagementFactory.getPlatformMBeanServer();
        HotSpotDiagnosticMXBean mxBean = ManagementFactory.newPlatformMXBeanProxy(
            server, "com.sun.management:type=HotSpotDiagnostic", HotSpotDiagnosticMXBean.class);
        mxBean.dumpThreads(filePath, HotSpotDiagnosticMXBean.ThreadDumpFormat.JSON);
        // mxBean.dumpHeap(filePath, live);
        return null;
    }

}