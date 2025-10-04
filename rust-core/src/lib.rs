use std::ffi::{CStr, CString};
use libc::c_char;
use pyo3::prelude::*;

#[no_mangle]
pub extern "C" fn bifrost_hello(input: *const c_char) -> *mut c_char {
    let c_str = unsafe { CStr::from_ptr(input) };
    let msg = c_str.to_str().unwrap_or("invalid utf8");
    let response = format!("Hello from Rust Bifröst, got: {}", msg);
    CString::new(response).unwrap().into_raw()
}
