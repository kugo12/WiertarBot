from psycopg2cffi import compat
compat.register()

from . import milestone, listeners, commands
