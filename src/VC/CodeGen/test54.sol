=== test program === 
/* prime.c  It prompts the user to enter an integer N. It prints out
 *           if it is a prime or not. If not, it prints out a factor of N.
 */

void prime(int n) {
  int i;
  boolean flag = true;

  putString("Enter value of N: ");
 
 for (i=2; (i<(n/2)) && flag; ) {
    if ( ((n / i) * i) == n) 
      flag = false;
    else
      i = i + 1;
  }
 
  if (flag) {
    putInt(n);
    putStringLn(" is prime");
  } else {
    putInt(n);
    putString(" has "); 
    putInt(i);
    putStringLn(" as a factor");
  }
  return;
}
int main() {
  prime(77);
  prime(79);
  return 0;
}

======= The VC compiler =======

Pass 1: Lexical and syntactic Analysis
Pass 2: Semantic Analysis
Pass 3: Code Generation

Compilation was successful.

=== The output of the test program === 
Enter value of N: 77 has 7 as a factor
Enter value of N: 79 is prime
