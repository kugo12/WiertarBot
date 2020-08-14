from selenium import webdriver
from webdriver_manager.chrome import ChromeDriverManager

import time
import imaplib
import hashlib
import os

from .config import email, password, gmail_password, cookie_path


def get_message_ids_from_gmail(M):
    _, ids = M.search(None, '(FROM "security@facebookmail.com")')

    return ids[0]


def FacebookUnlock():
    print('Unlocking account...')
    M = imaplib.IMAP4_SSL('imap.gmail.com', 993)
    M.login(email, gmail_password)
    M.select('INBOX')
    ids = get_message_ids_from_gmail(M)

    options = webdriver.ChromeOptions()
    options.add_argument('--disable-gpu')

    driver = webdriver.Chrome(executable_path=ChromeDriverManager().install(), options=options)
    driver.implicitly_wait(10)
    driver.get('https://www.facebook.com/')
    time.sleep(2)

    username_box = driver.find_element_by_id('email')
    username_box.send_keys(email)

    password_box = driver.find_element_by_id('pass')
    password_box.send_keys(password)

    password_box.submit()
    time.sleep(5)

    checkpointSubmit = lambda: driver.find_element_by_id('checkpointSubmitButton').click()

    checkpointSubmit()
    time.sleep(3)
    try:
        radio = driver.find_element_by_class_name('uiInputLabelLabel')
        radio.click()
        time.sleep(3)
        checkpointSubmit()
        time.sleep(3)
        checkpointSubmit()
        time.sleep(3)

        ids1 = ids
        tries = 0
        while ids1 == ids and tries < 100:
            M.select('INBOX')
            ids1 = get_message_ids_from_gmail(M)
            time.sleep(3)
            tries += 1

        id = ids1.split()[-1]
        key = M.fetch(id, '(BODY[TEXT])')[1][0][1].split(b'to ', 1)[1].split(b'.', 1)[0].decode()

        driver.find_element_by_name('captcha_response').send_keys(key)
        time.sleep(3)
        checkpointSubmit()
        time.sleep(3)
        checkpointSubmit()
        time.sleep(3)
    except:
        print('nie')

    psw = hashlib.sha256(password.encode()).hexdigest()[20:-24]
    driver.find_element_by_name('password_new').send_keys(psw)
    driver.find_element_by_name('password_confirm').send_keys(psw)
    time.sleep(3)
    checkpointSubmit()
    time.sleep(8)
    try:
        checkpointSubmit()
    except:
        print('nie ma')
    time.sleep(10)
    driver.quit()

    config_path = os.path.join(os.path.dirname(__file__), 'config/local_config.py')
    with open(config_path, 'r+') as f:
        text = f.read()
        text = text.replace(password, psw)
        f.truncate(0)
        f.seek(0)
        f.write(text)

    if os.path.exists(cookie_path):
        os.remove(cookie_path)

    print('Account unlocked...')
    time.sleep(20)
    return psw
