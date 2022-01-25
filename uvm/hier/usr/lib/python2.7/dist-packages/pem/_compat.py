

import sys


__all__ = ["ABC", "text_type", "PY2"]

PY2 = sys.version_info[0] == 2


if PY2:
    import abc

    text_type = str  # noqa

    class ABC(object, metaclass=abc.ABCMeta):
        pass


else:
    from abc import ABC

    text_type = str
