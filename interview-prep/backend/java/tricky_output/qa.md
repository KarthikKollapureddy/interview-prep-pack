# Tricky Output Prediction Questions — Java & JavaScript

> 20 "What's the output?" problems covering String pool, autoboxing, ==, collections, streams, JS coercion  
> Priority: **P0** — These appear in EVERY screening round and online assessments

---

## Java Output Questions

### Q1. String Pool & == vs equals()

```java
String s1 = "hello";
String s2 = "hello";
String s3 = new String("hello");
String s4 = new String("hello").intern();

System.out.println(s1 == s2);        // ?
System.out.println(s1 == s3);        // ?
System.out.println(s1.equals(s3));   // ?
System.out.println(s1 == s4);        // ?
```

<details>
<summary>Answer</summary>

```
true    — s1, s2 both point to same String pool reference
false   — s3 is a new object on heap, different reference
true    — equals() compares content
true    — intern() returns the pool reference
```
</details>

---

### Q2. String Concatenation

```java
String s1 = "hello" + "world";
String s2 = "helloworld";
String s3 = "hello";
String s4 = s3 + "world";

System.out.println(s1 == s2);   // ?
System.out.println(s2 == s4);   // ?
```

<details>
<summary>Answer</summary>

```
true    — compiler concatenates literals at compile time → same pool entry
false   — s4 involves a variable, so runtime StringBuilder is used → new object
```
</details>

---

### Q3. Integer Caching (Autoboxing)

```java
Integer a = 127;
Integer b = 127;
Integer c = 128;
Integer d = 128;
Integer e = new Integer(127);

System.out.println(a == b);   // ?
System.out.println(c == d);   // ?
System.out.println(a == e);   // ?
System.out.println(a.equals(e)); // ?
```

<details>
<summary>Answer</summary>

```
true    — Integer caches -128 to 127; both point to same cached object
false   — 128 is outside cache range; two different objects
false   — new Integer() always creates new object (deprecated in Java 9+)
true    — equals() compares value
```
</details>

---

### Q4. Autoboxing & Unboxing Traps

```java
Integer x = null;
int y = x;   // What happens?
```

<details>
<summary>Answer</summary>

```
NullPointerException at runtime!
Unboxing null Integer to int calls x.intValue() → NPE.
Compiler doesn't catch this.
```
</details>

---

### Q5. equals() and hashCode() Contract

```java
class Person {
    String name;
    Person(String name) { this.name = name; }
    @Override
    public boolean equals(Object o) {
        return o instanceof Person p && name.equals(p.name);
    }
    // NO hashCode() override!
}

Set<Person> set = new HashSet<>();
set.add(new Person("Alice"));
set.add(new Person("Alice"));
System.out.println(set.size());   // ?
```

<details>
<summary>Answer</summary>

```
2
Even though equals() returns true, hashCode() is NOT overridden.
Default hashCode() returns different values for different objects.
HashSet checks hashCode FIRST — different hash → different bucket → treats as different objects.
Rule: If you override equals(), you MUST override hashCode().
```
</details>

---

### Q6. HashMap with Mutable Keys

```java
List<String> key = new ArrayList<>(List.of("hello"));
Map<List<String>, String> map = new HashMap<>();
map.put(key, "value");

System.out.println(map.get(key));       // ?
key.add("world");  // mutate the key!
System.out.println(map.get(key));       // ?
System.out.println(map.size());         // ?
```

<details>
<summary>Answer</summary>

```
"value"   — key found normally
null      — after mutation, hashCode changed but entry is in old bucket
1         — entry still exists but is unreachable → memory leak!

Lesson: Never use mutable objects as HashMap keys.
```
</details>

---

### Q7. finally Block Execution

```java
public static int getValue() {
    try {
        return 1;
    } finally {
        return 2;
    }
}
System.out.println(getValue());  // ?
```

<details>
<summary>Answer</summary>

```
2
finally block ALWAYS executes. The return in finally overrides the return in try.
⚠️ This is a bad practice — never return from finally block.
```
</details>

---

### Q8. Exception Handling Order

```java
try {
    int[] arr = {1, 2, 3};
    System.out.println(arr[5]);
} catch (Exception e) {
    System.out.println("Exception");
} catch (ArrayIndexOutOfBoundsException e) {
    System.out.println("ArrayIndexOutOfBounds");
}
```

<details>
<summary>Answer</summary>

```
COMPILATION ERROR!
ArrayIndexOutOfBoundsException is a subclass of Exception.
The broader catch must come AFTER the specific one.
Java compiler detects unreachable catch block.
```
</details>

---

### Q9. Static & Instance Initialization Order

```java
class Parent {
    static { System.out.println("Parent static"); }
    { System.out.println("Parent instance"); }
    Parent() { System.out.println("Parent constructor"); }
}

class Child extends Parent {
    static { System.out.println("Child static"); }
    { System.out.println("Child instance"); }
    Child() { System.out.println("Child constructor"); }
}

public class Main {
    public static void main(String[] args) {
        new Child();
        System.out.println("---");
        new Child();
    }
}
```

<details>
<summary>Answer</summary>

```
Parent static
Child static
Parent instance
Parent constructor
Child instance
Child constructor
---
Parent instance
Parent constructor
Child instance
Child constructor

Static blocks run ONCE when class is first loaded (parent first).
Instance blocks + constructors run on EVERY new object (parent first).
```
</details>

---

### Q10. Method Overriding & Polymorphism

```java
class Animal {
    String name = "Animal";
    void sound() { System.out.println("Some sound"); }
}
class Dog extends Animal {
    String name = "Dog";
    void sound() { System.out.println("Bark"); }
}

Animal a = new Dog();
System.out.println(a.name);   // ?
a.sound();                      // ?
```

<details>
<summary>Answer</summary>

```
"Animal"   — fields are NOT polymorphic, resolved at compile time (reference type)
"Bark"     — methods ARE polymorphic, resolved at runtime (actual object type)

This is a CLASSIC interview question.
```
</details>

---

### Q11. Covariant Return Type & Overriding

```java
class A {
    Object getValue() { return "A"; }
}
class B extends A {
    @Override
    String getValue() { return "B"; }  // String is subtype of Object — valid!
}

A obj = new B();
System.out.println(obj.getValue());  // ?
```

<details>
<summary>Answer</summary>

```
"B"
Covariant return types allow overriding method to return a more specific type.
Method is polymorphic → runtime object (B) determines which method runs.
```
</details>

---

### Q12. Collections.unmodifiableList Trap

```java
List<String> original = new ArrayList<>(List.of("a", "b"));
List<String> unmod = Collections.unmodifiableList(original);

original.add("c");  // modify original
System.out.println(unmod.size());  // ?
System.out.println(unmod);         // ?
```

<details>
<summary>Answer</summary>

```
3
[a, b, c]

unmodifiableList creates a VIEW over the original list, not a copy!
Modifications to the original ARE visible through the unmodifiable view.
Use List.copyOf(original) for a true immutable copy (Java 10+).
```
</details>

---

### Q13. Stream Operations & Laziness

```java
List<String> list = List.of("a", "bb", "ccc", "dddd");

long count = list.stream()
    .filter(s -> {
        System.out.println("filter: " + s);
        return s.length() > 1;
    })
    .map(s -> {
        System.out.println("map: " + s);
        return s.toUpperCase();
    })
    .findFirst()
    .stream().count();

System.out.println("Result: " + count);
```

<details>
<summary>Answer</summary>

```
filter: a
filter: bb
map: bb
Result: 1

Streams are LAZY. "a" doesn't pass filter → next element.
"bb" passes filter → goes to map → findFirst() short-circuits → STOPS.
"ccc" and "dddd" are never processed.
```
</details>

---

### Q14. ConcurrentModificationException

```java
List<String> list = new ArrayList<>(List.of("a", "b", "c", "d"));

for (String s : list) {
    if (s.equals("b")) {
        list.remove(s);
    }
}
System.out.println(list);
```

<details>
<summary>Answer</summary>

```
ConcurrentModificationException (usually!)

Enhanced for-loop uses Iterator internally. Modifying the list while iterating
triggers fail-fast behavior. 

Fix options:
1. list.removeIf(s -> s.equals("b"));  // ✅ Java 8+
2. Use Iterator.remove() explicitly
3. Use CopyOnWriteArrayList
```
</details>

---

### Q15. try-with-resources Execution Order

```java
class MyResource implements AutoCloseable {
    String name;
    MyResource(String name) {
        this.name = name;
        System.out.println("Open " + name);
    }
    @Override
    public void close() { System.out.println("Close " + name); }
}

try (MyResource a = new MyResource("A");
     MyResource b = new MyResource("B")) {
    System.out.println("Body");
    throw new RuntimeException("Error");
} catch (Exception e) {
    System.out.println("Catch: " + e.getMessage());
} finally {
    System.out.println("Finally");
}
```

<details>
<summary>Answer</summary>

```
Open A
Open B
Body
Close B
Close A
Catch: Error
Finally

Resources are closed in REVERSE order (LIFO) BEFORE catch and finally blocks.
```
</details>

---

## JavaScript Output Questions

### Q16. Type Coercion Madness

```javascript
console.log(1 + "2");          // ?
console.log("5" - 3);          // ?
console.log(true + true);      // ?
console.log("5" + 3 - 2);     // ?
console.log([] + []);          // ?
console.log([] + {});          // ?
console.log({} + []);          // ?
```

<details>
<summary>Answer</summary>

```
"12"           — number + string = string concatenation
2              — string - number = numeric subtraction (- doesn't concat)
2              — true is 1, so 1 + 1 = 2
51             — "5" + 3 = "53" (string), then "53" - 2 = 51 (number)
""             — both arrays toString() → "" + "" = ""
"[object Object]"  — [] → "" + {} → "[object Object]"
0 (in console) — {} treated as empty block, +[] → 0 (varies by context)
```
</details>

---

### Q17. var, let, const Hoisting

```javascript
console.log(a);   // ?
console.log(b);   // ?

var a = 1;
let b = 2;
```

<details>
<summary>Answer</summary>

```
undefined          — var is hoisted, initialized to undefined
ReferenceError     — let is hoisted but in "temporal dead zone" (TDZ), can't access before declaration
```
</details>

---

### Q18. Closures & setTimeout

```javascript
for (var i = 0; i < 3; i++) {
    setTimeout(() => console.log(i), 100);
}
```

<details>
<summary>Answer</summary>

```
3
3
3

var is function-scoped. By the time setTimeout fires, the loop is done and i = 3.
All three callbacks share the SAME i variable.

Fix: Use let (block-scoped) instead of var:
for (let i = 0; i < 3; i++) {
    setTimeout(() => console.log(i), 100);  // 0, 1, 2
}
```
</details>

---

### Q19. == vs === and Falsy Values

```javascript
console.log(0 == false);       // ?
console.log(0 === false);      // ?
console.log("" == false);      // ?
console.log(null == undefined); // ?
console.log(null === undefined);// ?
console.log(NaN == NaN);       // ?
```

<details>
<summary>Answer</summary>

```
true       — == coerces: false → 0, then 0 == 0
false      — === checks type: number !== boolean
true       — == coerces: "" → 0, false → 0, then 0 == 0
true       — special rule: null == undefined is true
false      — different types
false      — NaN is not equal to anything, including itself!

Use Number.isNaN(x) instead of x === NaN.
```
</details>

---

### Q20. this Keyword & Arrow Functions

```javascript
const obj = {
    name: "Alice",
    greet: function() {
        console.log(this.name);
    },
    greetArrow: () => {
        console.log(this.name);
    },
    greetDelay: function() {
        setTimeout(function() {
            console.log(this.name);
        }, 100);
        setTimeout(() => {
            console.log(this.name);
        }, 100);
    }
};

obj.greet();        // ?
obj.greetArrow();   // ?
obj.greetDelay();   // ?
```

<details>
<summary>Answer</summary>

```
"Alice"      — regular function: this = obj (caller)
undefined    — arrow function: this = enclosing scope (module/window), not obj
undefined    — setTimeout regular function: this = window/global
"Alice"      — setTimeout arrow function: this = inherited from greetDelay, which is obj

Rule: Arrow functions don't have their own 'this'. They capture it from enclosing lexical scope.
```
</details>

---

## Quick Reference: Java Gotchas Cheat Sheet

| Trap | Wrong Assumption | Reality |
|------|------------------|---------|
| `Integer == Integer` | Always compares values | Only cached for -128 to 127 |
| `String == String` | Compares content | Compares references (use `.equals()`) |
| `finally` block | Skipped if return in try | Always executes (even with return) |
| `unmodifiableList` | Creates independent copy | Creates view of original list |
| `ConcurrentModification` | Can remove in for-each | Must use `Iterator.remove()` or `removeIf()` |
| Field access | Polymorphic like methods | Resolved by reference type (compile time) |
| `@Override` return type | Must match exactly | Can return subtype (covariant) |
| Static blocks | Run every instantiation | Run once when class is loaded |
| `hashCode` not overridden | equals still works in Set | HashSet uses hashCode first → broken |
| Stream operations | Execute immediately | Lazy — execute only on terminal operation |
