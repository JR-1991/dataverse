from locust_settings import get_settings_info, KEY_PERSISTENT_IDS
import random


def get_locust_request_kwargs():
    return dict(verify=False,)   # allow a self-signed certificate


def homepage(l):
    msg('> homepage')
    l.client.get('/', **get_locust_request_kwargs())


def random_dataset_page(l):
    msg('> random_dataset_page')

    persistent_ids = get_settings_info(KEY_PERSISTENT_IDS)
    assert (
        persistent_ids is not None
    ), f'No values found in creds file for {KEY_PERSISTENT_IDS}'

    assert (
        len(persistent_ids) > 0
    ), f'No values found in creds file for list {KEY_PERSISTENT_IDS}'


    random_id = random.choice(persistent_ids)

    dataset_url = f'/dataset.xhtml?persistentId={random_id}'

    l.client.get(dataset_url, **get_locust_request_kwargs())


def msg(s): print s
