=== test program === 
/* bubble.c -- Read an integer array, print it, then sort it and
 * print it. Use the bubble sort method.
 */

void printIntArray(int a[], int n)
     /* n is the number of elements in the array a.
      * These values are printed out, five per line. */
{
  int i;

  for (i=0; i<n; i=i+1) {
    putInt(a[i]);
    putString(" ");
  }
  putLn();
  return;
}

void bubbleSort(int a[], int n)
/* It sorts in non-decreasing order the first N positions of A. It uses 
 * the bubble sort method.
 */
{
  int lcv;
  int limit = n-1;
  int temp;
  int lastChange;
  
  while (limit > 0) {
    lastChange = 0;
    for (lcv=0;lcv<limit;lcv=lcv+1)
      /* Notice that the values in positions LIMIT+1 .. N are in
       * their final position, i.e. they are sorted right */
	if (a[lcv]>a[lcv+1]) {
	  temp = a[lcv];
	  a[lcv] = a[lcv+1];
	  a[lcv+1] = temp;
	  lastChange = lcv;
	}
    limit = lastChange;
  }
  return;
}

int main() {
  int x[10] = {3, 10, 1, 5, 8, 0, 20, 1, 4, 100};
  int hmny = 10;
  int who;
  int where;

  putStringLn("The array was:");
  printIntArray(x,hmny);
  bubbleSort(x,hmny);
  putStringLn("The sorted array is:");
  printIntArray(x,hmny);
  return 0;

}

======= The VC compiler =======

Pass 1: Lexical and syntactic Analysis
Pass 2: Semantic Analysis
Pass 3: Code Generation

Compilation was successful.

=== The output of the test program === 
The array was:
3 10 1 5 8 0 20 1 4 100 
The sorted array is:
0 1 1 3 4 5 8 10 20 100 
