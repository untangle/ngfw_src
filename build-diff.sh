#! /bin/zsh

jardiff() {
  diff -u <(jar tvf $1 | awk '{print $8}') <(jar tvf $2 | awk '{print $8}')
}

f=$(mktemp)

diff -u -x '*.log' -r dist.bak dist > $f 2>&1

grep -vE '^Binary files .+\.jar differ$' $f

grep -E '^Binary files .+\.jar differ$' $f | while read meh blah jar1 foo jar2 bah ; do
  jardiff ${jar1} ${jar2}
done


