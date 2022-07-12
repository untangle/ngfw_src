from uvm import login_tools
from unittest.mock import patch, MagicMock
import unittest


class TestTokenValid(unittest.TestCase):

    @patch('requests.post')
    def test_token_validity(self, mock_post):
        post_result = MagicMock(return_value=None)
        post_result.raise_for_status = lambda: None
        post_result.json = lambda: True
        self.assertTrue(login_tools.valid_token(None, 'token'))
