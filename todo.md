# Todo

## Performance

The command suggestions are pretty sluggish, probably because they always call the db.
Maybe we should do something like a updating cache.

i would like to see whether its possible to hook into exposed and directly track the db instead
of manually hooking into our current modifications of the table.
