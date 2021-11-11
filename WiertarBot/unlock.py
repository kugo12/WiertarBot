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


def click_by_aria_label(driver, label):
    driver.find_element_by_xpath(f"//div[@aria-label='{ label }']").click()


def FacebookUnlock():
    print('Unlocking account...')
    M = imaplib.IMAP4_SSL('imap.gmail.com', 993)
    M.login(email, gmail_password)
    M.select('INBOX')
    ids = get_message_ids_from_gmail(M)

    options = webdriver.ChromeOptions()
    options.add_argument('--disable-gpu')
    options.add_argument("--disable-dev-shm-usage")
    options.add_argument("--no-sandbox")
    options.add_argument("--headless")
    options.add_argument("--window-size=1400,600")
    options.headless = True

    driver = webdriver.Chrome(options=options)
    driver.implicitly_wait(10)
    driver.get('https://www.facebook.com/')
    time.sleep(5)

    username_box = driver.find_element_by_id('email')
    username_box.send_keys(email)

    password_box = driver.find_element_by_id('pass')
    password_box.send_keys(password)

    password_box.submit()
    time.sleep(5)

    click_by_aria_label(driver, "Rozpocznij")
    time.sleep(5)
    click_by_aria_label(driver, "Dalej")
    time.sleep(5)
    
    driver.find_element_by_xpath("//span[text()='Uzyskaj kod w e-mailu']").click()
    time.sleep(5)
    click_by_aria_label(driver, "Uzyskaj kod")

    ids1 = ids
    tries = 0
    while ids1 == ids and tries < 100:
        M.select('INBOX')
        ids1 = get_message_ids_from_gmail(M)
        time.sleep(5)
        tries += 1

    id = ids1.split()[-1]
    key = M.fetch(id, '(BODY[TEXT])')[1][0][1].split(b'to ', 1)[1].split(b'.', 1)[0].decode()

    driver.find_element_by_xpath("//label[@aria-label='Wprowadź kod']//input[@type='text']").send_keys(key)
    time.sleep(5)
    click_by_aria_label(driver, "Potwierdź")
    time.sleep(5)
    click_by_aria_label(driver, "Dalej")
    time.sleep(5)
    click_by_aria_label(driver, "Dalej")
    time.sleep(5)

    psw = hashlib.sha256(password.encode()).hexdigest()[20:-24]
    
    driver.find_element_by_xpath("//label[@aria-label='Podaj nowe hasło']//input[@type='password']").send_keys(psw)
    time.sleep(5)
    click_by_aria_label(driver, "Zapisz zmiany")
    time.sleep(8)
    try:
        click_by_aria_label(driver, "Dalej")
        time.sleep(10)
    except:
        pass
    driver.quit()

    config_path = os.path.join(os.path.dirname(__file__), 'config/local_config.py')
    with open(config_path, 'r+') as f:
        text = f.read()
        text = text.replace(password, psw)
        f.truncate(0)
        f.seek(0)
        f.write(text)

    if cookie_path.exists():
        cookie_path.unlink()

    print('Account unlocked')
    time.sleep(20)
    return psw
